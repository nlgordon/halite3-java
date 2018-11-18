package com.insanedev.hlt


import groovy.transform.EqualsAndHashCode

enum ShipStatus {
    EXPLORING,NAVIGATING
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
        if (status == ShipStatus.NAVIGATING) {
            return getNavigationMove()
        } else if (status == ShipStatus.EXPLORING) {
            return getExplorationMove()
        }

        return stayStill()
    }

    MoveCommand getExplorationMove() {
        PossibleMove recommendedMove = position.possibleMoves()
                .map({ new PossibleMove(it.first, it.second, game.gameMap[it.second]) })
                .filter({ it.mapCell.halite > 0 })
                .sorted({ PossibleMove left, PossibleMove right -> right.mapCell.halite.compareTo(left.mapCell.halite) })
                .filter({ canMoveToPosition(it.position) })
                .findFirst()
                .orElse(new PossibleMove(Direction.STILL, position, game.gameMap[position]))

        return move(recommendedMove.direction)
    }

    MoveCommand getNavigationMove() {
        MoveCommand move
        if (!destination || destination == position) {
            move = stayStill()
        }

        int dx = destination.x - position.x
        int dy = destination.y - position.y

        int absoluteDistance = Math.abs(dx) + Math.abs(dy)

        if (absoluteDistance == 0) {
            move = stayStill()
        } else if (absoluteDistance == 1 && !canMoveToPosition(destination)) {
            move = stayStill()
        } else {
            if (Math.abs(dx) >= Math.abs(dy)) {
                if (dx > 0) {
                    move = getBestNavigationStep(Direction.EAST)
                } else {
                    move = getBestNavigationStep(Direction.WEST)
                }
            } else {
                if (dy > 0) {
                    move = getBestNavigationStep(Direction.SOUTH)
                } else {
                    move = getBestNavigationStep(Direction.NORTH)
                }
            }
        }
        return move
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
