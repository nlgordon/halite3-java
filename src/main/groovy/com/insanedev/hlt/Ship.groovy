package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game
    Position destination

    Ship(Game game, final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
        this.game = game
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
        return Command.move(id, direction)
    }

    MoveCommand stayStill() {
        assertActive()
        return Command.move(id, Direction.STILL)
    }

    MoveCommand navigate() {
        if (!destination || destination == position) {
            return stayStill()
        }

        int dx = destination.x - position.x
        int dy = destination.y - position.y

        int absoluteDistance = Math.abs(dx) + Math.abs(dy)

        if (absoluteDistance == 0) {
            return stayStill()
        } else if (absoluteDistance == 1 && !canMoveToPosition(destination)) {
            return stayStill()
        }else {
            if (Math.abs(dx) >= Math.abs(dy)) {
                if (dx > 0) {
                    return getBestNavigationStep(Direction.EAST)
                } else {
                    return getBestNavigationStep(Direction.WEST)
                }
            } else {
                if (dy > 0) {
                    return getBestNavigationStep(Direction.SOUTH)
                } else {
                    return getBestNavigationStep(Direction.NORTH)
                }
            }
        }
    }

    MoveCommand getBestNavigationStep(Direction direction) {
        if (canMoveInDirection(direction)) {
            return move(direction)
        }

        def perpendiculars = direction.getPerpendiculars()

        return perpendiculars.stream()
                .filter({ canMoveInDirection(it) })
                .map({ move(it) })
                .findFirst()
                .orElse(stayStill())
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
}
