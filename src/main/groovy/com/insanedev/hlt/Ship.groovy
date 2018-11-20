package com.insanedev.hlt


import groovy.transform.EqualsAndHashCode

import java.util.stream.Stream

enum ShipStatus {
    EXPLORING, NAVIGATING
}

@EqualsAndHashCode
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game
    Position destination
    ShipStatus status = ShipStatus.EXPLORING

    Ship(Game game, final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
        this.game = game
        game.gameMap[position].ship = this
    }

    boolean isFull() {
        return halite >= Constants.MAX_HALITE
    }

    ConstructDropoffCommand makeDropoff() {
        assertActive()
        return Command.transformShipIntoDropoffSite(id)
    }

    MoveCommand move(final Direction direction) {
        assertActive()
        if (direction != Direction.STILL) {
            game.gameMap[position].ship = null
        }
        game.gameMap[position.directionalOffset(direction)].ship = this
        return Command.move(id, direction)
    }

    MoveCommand stayStill() {
        assertActive()
        return Command.move(id, Direction.STILL)
    }

    void setDestination(Position position) {
        this.destination = position
        this.status = ShipStatus.NAVIGATING
    }

    PossibleMove getDesiredMove() {
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
        return possibleCardinalMoves()
                .filter({ it.mapCell.halite > 0 })
                .sorted({ PossibleMove left, PossibleMove right -> right.mapCell.halite.compareTo(left.mapCell.halite) })
                .filter({ it.ableToMove })
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
        if (destination) {
            int dx = destination.x - position.x
            int dy = destination.y - position.y

            int absoluteDistance = Math.abs(dx) + Math.abs(dy)

            if (absoluteDistance == 0) {
                direction = Direction.STILL
            } else if (absoluteDistance == 1 && !canMoveToPosition(destination)) {
                direction = Direction.STILL
            } else {
                direction = getDesiredDirection(dx, dy)
            }
        }
        return direction
    }

    Direction getDesiredDirection(int dx, int dy) {
        Direction direction = Direction.STILL
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (dx > 0) {
                direction = Direction.EAST
            } else if (dx < 0) {
                direction = Direction.WEST
            }
        } else if (Math.abs(dx) < Math.abs(dy)) {
            if (dy > 0) {
                direction = Direction.SOUTH
            } else if (dy < 0) {
                direction = Direction.NORTH
            }
        }
        return direction
    }


    PossibleMove createPossibleMove(Direction direction) {
        def newPosition = position.directionalOffset(direction)
        return new PossibleMove(direction, newPosition, game.gameMap[newPosition], this)
    }

    boolean canMoveToPosition(Position newPosition) {
        return !game.gameMap[newPosition].occupied || newPosition == position
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

    PossibleMove(Direction direction, Position position, MapCell mapCell, Ship ship) {
        this.direction = direction
        this.position = position
        this.mapCell = mapCell
        this.ship = ship
    }

    boolean isAbleToMove() {
        return !mapCell.occupied || mapCell.ship == ship
    }

    MoveCommand executeMove() {
        return ship.move(direction)
    }

    PossibleMove getAlternateRoute() {
        return ship.getAlternateRoute(direction)
    }
}
