package com.insanedev.hlt

import com.insanedev.*
import groovy.transform.EqualsAndHashCode
import reactor.core.publisher.Flux

@EqualsAndHashCode(callSuper = true)
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game
    Position destination
    int minCellAmount = Configurables.MIN_CELL_AMOUNT
    int fullAmount = Constants.MAX_HALITE
    Position positionMovingTo
    List<ShipHistory> history = []
    Mission mission

    Ship(Game game, final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
        this.game = game
        game.gameMap[position].ship = this
        if (FeatureFlags.getFlagStatus(player, "LOWER_MIN_CELL_AMOUNT")) {
            minCellAmount = minCellAmount / 2
        }

        this.mission = new ExplorationMission(this)
    }

    boolean isFull() {
        return halite >= fullAmount
    }

    ConstructDropoffCommand makeDropoff() {
        assertActive()
        return Command.transformShipIntoDropoffSite(id)
    }

    MoveCommand move(final Direction direction) {
        assertActive()
        if (direction != Direction.STILL) {
            Log.log("Moving $id ${direction} from ${position} ship halite: $halite cell halite: ${game.gameMap[position].halite}")
            def cell = game.gameMap[position]
            if (cell.ship == this) {
                cell.ship = null
            }
        }

        positionMovingTo = position.directionalOffset(direction)
        game.gameMap[positionMovingTo].ship = this
        return Command.move(id, direction)
    }

    void setNavigationDestination(Position position) {
        this.mission = new NavigationMission(this, position)
    }

    PossibleMove getDesiredMove() {
        assertActive()
        if (mission) {
            return mission.desiredMove
        }
        return createPossibleMove(Direction.STILL)
    }

    PossibleMove getAlternateRoute(Direction direction) {
        def perpendiculars = direction.getPerpendiculars()
        return perpendiculars.stream()
                .map({ createPossibleMove(it) })
                .filter({ it.ableToMove })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    boolean hasHaliteToMove() {
        return halite >= (game.gameMap[position].halite * 0.1)
    }

    int calculateDistance(Position other) {
        return game.gameMap.calculateDistance(position, other)
    }

    PossibleMove createPossibleMove(Direction direction) {
        def newPosition = position.directionalOffset(direction)
        return new PossibleMove(direction, newPosition, game.gameMap[newPosition], this)
    }

    void destroy() {
        Log.log("Destroyed Ship $id")
        destroyed = true
        def cell = game.gameMap[this]
        if (cell.ship == this) {
            cell.ship = null
        }
        if (positionMovingTo) {
            cell = game.gameMap[positionMovingTo]
            if (cell.ship == this) {
                cell.ship = null
            }
        }
    }

    boolean isActive() {
        return !destroyed
    }

    void assertActive() {
        assert !destroyed
    }

    void update(Position newPosition, int newHalite) {
        ShipHistoryAction type
        def haliteDelta = 0
        if (newHalite > halite) {
            haliteDelta = newHalite - halite
            type = ShipHistoryAction.HARVEST
        } else if (newHalite == 0 && halite > 0) {
            haliteDelta = newHalite - halite
            type = ShipHistoryAction.DROPOFF
        } else if (newPosition != position) {
            type = ShipHistoryAction.MOVE
        } else {
            type = ShipHistoryAction.STILL
        }

        history[game.turnNumber] = new ShipHistory(game.turnNumber, newHalite, haliteDelta, newPosition, type)

        if (game.gameMap[position].ship == this) {
            game.gameMap[position].ship = null
        }
        game.gameMap[newPosition].ship = this
        this.position = newPosition
        this.halite = newHalite

        if (mission.complete) {
            mission = mission.nextMission
        }
    }

    boolean getInspired() {
        Flux<Position> positionsToCheck = Flux.empty()
        for (int i = 1; i < 5; i++) {
            int dx = i
            int dy = 0
            while (dx >= 0) {
                positionsToCheck = positionsForMutations(positionsToCheck, dx, dy)
                dx--
                dy++
            }
        }

        long otherShips = positionsToCheck.distinct().filter({ game.gameMap[it].occupied }).count().block()
        if (otherShips >= 2) {
            return true
        }
        return false
    }

    private Flux<Position> positionsForMutations(Flux<Position> positionsToCheck, int dx, int dy) {
        List<Tuple2<Integer, Integer>> mutations = [new Tuple2(1, 1), new Tuple2(1, -1), new Tuple2(-1, -1), new Tuple2(-1, 1)]
        positionsToCheck = Flux.concat(positionsToCheck, Flux.fromIterable(mutations).map({
            position.offset(it.first * dx, it.second * dy)
        }))
        return positionsToCheck
    }

    int getHaliteAtCurrentPosition() {
        game.gameMap[position].halite
    }

    InfluenceVector getInfluence() {
        player.getInfluence(this)
    }

    String toString() {
        return "id:$id $position halite:$halite"
    }
}

class PossibleMove {
    Direction direction
    Position position
    MapCell mapCell
    Ship ship
    MoveCommand command
    InfluenceVector influence

    PossibleMove(Direction direction, Position position, MapCell mapCell, Ship ship, InfluenceVector influence = InfluenceVector.ZERO) {
        this.direction = direction
        this.position = position
        this.mapCell = mapCell
        this.ship = ship
        this.influence = influence
    }

    boolean isAbleToMove() {
        if (mapCell.ship == ship) {
            return true
        }
        return (!mapCell.occupied && !mapCell.isNearOpponent(ship.player))
    }

    boolean isAbleToMoveOrNavigating() {
        return ableToMove || mapCell?.ship?.player == ship.player
    }

    void executeIfAble() {
        if (ableToMove) {
            executeMove()
        }
    }

    boolean isExecuted() {
        return command != null
    }

    MoveCommand executeMove() {
        return command = ship.move(direction)
    }

    PossibleMove getAlternateRoute() {
        return ship.getAlternateRoute(direction)
    }

    // TODO: Influence doesn't belong here
    int getHalite() {
        return mapCell.halite + influence.appliedToDirection(direction)
    }

    String toString() {
        return "PossibleMove: ${ship.id} ${ship.position} -> $direction $position ${mapCell.ship == null}"
    }
}

enum ShipHistoryAction {
    HARVEST, DROPOFF, MOVE, STILL
}

class ShipHistory {
    int turn
    int halite
    int haliteDelta
    Position position
    ShipHistoryAction type

    ShipHistory(int turn, int halite, int haliteDelta, Position position, ShipHistoryAction type) {
        this.turn = turn
        this.halite = halite
        this.haliteDelta = haliteDelta
        this.position = position
        this.type = type
    }
}
