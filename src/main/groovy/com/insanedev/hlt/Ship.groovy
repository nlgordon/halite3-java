package com.insanedev.hlt

import com.insanedev.Configurables
import com.insanedev.InfluenceVector
import groovy.transform.EqualsAndHashCode

import java.util.stream.Stream

enum ShipStatus {
    EXPLORING, NAVIGATING, HOLDING
}

@EqualsAndHashCode(callSuper = true)
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game
    Position destination
    ShipStatus status = ShipStatus.EXPLORING
    int minHarvestAmount = Configurables.MIN_HARVEST_AMOUNT
    int fullAmount = Constants.MAX_HALITE
    Position positionMovingTo
    List<ShipHistory> history = []

    Ship(Game game, final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
        this.game = game
        game.gameMap[position].ship = this
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

    void setDestination(Position position) {
        this.destination = position
        this.status = ShipStatus.NAVIGATING
    }

    PossibleMove getDesiredMove(InfluenceVector influence) {
        assertActive()
        PossibleMove desiredMove
        if (status == ShipStatus.NAVIGATING) {
            desiredMove = getDesiredNavigationMove()
        } else if (status == ShipStatus.EXPLORING) {
            desiredMove = getExplorationMove(influence)
        } else {
            desiredMove = createPossibleMove(Direction.STILL)
        }
        return desiredMove
    }

    PossibleMove getDesiredMove() {
        return getDesiredMove(InfluenceVector.ZERO)
    }

    PossibleMove getExplorationMove(InfluenceVector influence) {
        def currentCellHalite = game.gameMap[position].halite
        if (currentCellHalite * 0.25 >= minHarvestAmount || !hasHaliteToMove()) {
            return createPossibleMove(Direction.STILL)
        }
        return possibleCardinalMoves()
                // TODO: Move this up a step
                .map({it.influence = influence; it})
                .filter({ it.halite > currentCellHalite || currentCellHalite == 0 })
                .sorted({ PossibleMove left, PossibleMove right -> right.halite.compareTo(left.halite) })
                .filter({ it.ableToMoveOrNavigating })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    PossibleMove getAlternateRoute(Direction direction) {
        def perpendiculars = direction.getPerpendiculars()
        return perpendiculars.stream()
                .map({ createPossibleMove(it) })
                .filter({ it.ableToMove })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    PossibleMove getDesiredNavigationMove() {
        return createPossibleMove(getNavigationDirection())
    }

    Direction getNavigationDirection() {
        Direction direction = Direction.STILL
        if (!hasHaliteToMove()) {
            Log.debug("Unable to move $id due to too little halite of $halite for cell ${game.gameMap[position].halite}")
            return direction
        }
        if (destination) {
            int absoluteDistance = calculateDistance(destination)

            if (absoluteDistance == 0) {
                direction = Direction.STILL
            } else {
                direction = getDesiredNavigationDirection()
            }
        }
        return direction
    }

    boolean hasHaliteToMove() {
        return halite >= (game.gameMap[position].halite * 0.1)
    }

    int calculateDistance(Position other) {
        return game.gameMap.calculateDistance(position, other)
    }

    Direction getDesiredNavigationDirection() {
        int dx = destination.x - position.x
        int dy = destination.y - position.y
        int absDx = Math.abs(dx)
        int absDy = Math.abs(dy)
        int wrapped_dx = game.gameMap.width - absDx
        int wrapped_dy = game.gameMap.width - absDy

        Direction direction = Direction.STILL

        if (absDx >= absDy) {
            if (dx > 0) {
                direction = absDx < wrapped_dx ? Direction.EAST : Direction.WEST
            } else if (dx < 0) {
                direction = absDx < wrapped_dx ? Direction.WEST : Direction.EAST
            }
        } else if (absDx < absDy) {
            if (dy > 0) {
                direction = absDy < wrapped_dy ? Direction.SOUTH : Direction.NORTH
            } else if (dy < 0) {
                direction = absDy < wrapped_dy ? Direction.NORTH : Direction.SOUTH
            }
        }
        return direction
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

        if (position == destination && status == ShipStatus.NAVIGATING) {
            status = ShipStatus.EXPLORING
        } else if (full) {
            setDestination(game.me.shipyard.position)
        }
    }

    Stream<PossibleMove> possibleCardinalMoves() {
        return Direction.ALL_CARDINALS.stream()
                .map({ createPossibleMove(it) })
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
        return !mapCell.occupied || mapCell.ship == ship
    }

    boolean isAbleToMoveOrNavigating() {
        return ableToMove || mapCell.ship.status == ShipStatus.NAVIGATING
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
