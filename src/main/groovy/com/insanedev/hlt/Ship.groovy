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

    MoveCommand navigate() {
        return move(decideMove().direction)
    }

    PossibleMove decideMove() {
        PossibleMove decidedMove
        if (status == ShipStatus.NAVIGATING) {
            decidedMove = getNavigationMove()
        } else if (status == ShipStatus.EXPLORING) {
            decidedMove = getExplorationMove()
        } else {
            decidedMove = createPossibleMove(Direction.STILL)
        }
        return decidedMove
    }

    PossibleMove getExplorationMove() {
        return possibleCardinalMoves()
                .filter({ it.mapCell.halite > 0 })
                .sorted({ PossibleMove left, PossibleMove right -> right.mapCell.halite.compareTo(left.mapCell.halite) })
                .filter({ canMoveToPosition(it.position) })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    PossibleMove getNavigationMove() {
        PossibleMove possible = createPossibleMove(Direction.STILL)
        if (!destination || destination == position) {
            return possible
        }

        int dx = destination.x - position.x
        int dy = destination.y - position.y

        int absoluteDistance = Math.abs(dx) + Math.abs(dy)

        if (absoluteDistance == 0) {
            return possible
        } else if (absoluteDistance == 1 && !canMoveToPosition(destination)) {
            return possible
        } else {
            if (Math.abs(dx) >= Math.abs(dy)) {
                if (dx > 0) {
                    possible = getBestNavigationStep(Direction.EAST)
                } else if (dx < 0) {
                    possible = getBestNavigationStep(Direction.WEST)
                }
            } else if (Math.abs(dx) < Math.abs(dy)) {
                if (dy > 0) {
                    possible = getBestNavigationStep(Direction.SOUTH)
                } else if (dy < 0) {
                    possible = getBestNavigationStep(Direction.NORTH)
                }
            }
        }
        return possible
    }

    PossibleMove getBestNavigationStep(Direction direction) {
        if (canMoveInDirection(direction)) {
            return createPossibleMove(direction)
        }

        def perpendiculars = direction.getPerpendiculars()

        return perpendiculars.stream()
                .filter({ canMoveInDirection(it) })
                .map({ createPossibleMove(it) })
                .findFirst()
                .orElse(createPossibleMove(Direction.STILL))
    }

    PossibleMove createPossibleMove(Direction direction) {
        def newPosition = position.directionalOffset(direction)
        return new PossibleMove(direction, newPosition, game.gameMap[newPosition])
    }

    boolean canMoveInDirection(Direction direction) {
        Position newPosition = position.directionalOffset(direction)
        return canMoveToPosition(newPosition)
    }

    boolean canMoveToPosition(Position newPosition) {
        return !game.gameMap[newPosition].occupied
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

    PossibleMove(Direction direction, Position position, MapCell mapCell) {
        this.direction = direction
        this.position = position
        this.mapCell = mapCell
    }
}
