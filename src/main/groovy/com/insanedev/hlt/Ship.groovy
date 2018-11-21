package com.insanedev.hlt


import groovy.transform.EqualsAndHashCode

import java.util.stream.Stream

enum ShipStatus {
    EXPLORING, NAVIGATING
}

@EqualsAndHashCode(callSuper = true)
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game
    Position destination
    ShipStatus status = ShipStatus.EXPLORING
    int minHarvestAmount = 25
    int fullAmount = Constants.MAX_HALITE

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
            game.gameMap[position].ship = null
        }
        game.gameMap[position.directionalOffset(direction)].ship = this
        return Command.move(id, direction)
    }

    void setDestination(Position position) {
        this.destination = position
        this.status = ShipStatus.NAVIGATING
    }

    PossibleMove getDesiredMove() {
        assertActive()
        PossibleMove desiredMove
        if (status == ShipStatus.NAVIGATING) {
            desiredMove = getDesiredNavigationMove()
        } else if (status == ShipStatus.EXPLORING) {
            desiredMove = getExplorationMove()
        } else {
            desiredMove = createPossibleMove(Direction.STILL)
        }
        return desiredMove
    }

    PossibleMove getExplorationMove() {
        if (game.gameMap[position].halite * 0.25 > minHarvestAmount || !hasHaliteToMove()) {
            return createPossibleMove(Direction.STILL)
        }
        return possibleCardinalMoves()
                .filter({ it.mapCell.halite > 0 })
                .sorted({ PossibleMove left, PossibleMove right -> right.mapCell.halite.compareTo(left.mapCell.halite) })
                .filter({ it.ableToMove })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    PossibleMove getAlternateRoute(Direction direction) {
        // TODO: WTF is going on here to require this if
        if (direction == Direction.STILL) {
            return createPossibleMove(Direction.STILL)
        }
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
            Log.log("Unable to move $id due to too little halite of $halite for cell ${game.gameMap[position].halite}")
            return direction
        }
        if (destination) {
            int absoluteDistance = calculateDistance(destination)

            if (absoluteDistance == 0) {
                direction = Direction.STILL
            } else {
                direction = getDesiredDirection()
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

    Direction getDesiredDirection() {
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
        destroyed = true
        game.gameMap[this].ship = null
    }

    boolean isActive() {
        return !destroyed
    }

    void assertActive() {
        assert !destroyed
    }

    void update(Position newPosition, int newHalite) {
        game.gameMap[position].ship = null
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
}

class PossibleMove {
    Direction direction
    Position position
    MapCell mapCell
    Ship ship
    MoveCommand command

    PossibleMove(Direction direction, Position position, MapCell mapCell, Ship ship) {
        this.direction = direction
        this.position = position
        this.mapCell = mapCell
        this.ship = ship
    }

    boolean isAbleToMove() {
        return !mapCell.occupied || mapCell.ship == ship
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
}
