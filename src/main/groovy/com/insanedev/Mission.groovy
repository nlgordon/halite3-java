package com.insanedev

import com.insanedev.hlt.*
import reactor.core.publisher.Flux
import reactor.math.MathFlux

abstract class Mission {
    Ship ship

    Mission(Ship ship) {
        this.ship = ship
    }

    abstract boolean isComplete()

    abstract PossibleMove getDesiredMove()

    abstract PossibleMove getAlternateMove()

    abstract Position getDestination()

    abstract Mission getNextMission()
}

class ExplorationMission extends Mission {
    int minCellAmount = Configurables.MIN_CELL_AMOUNT

    // TODO: inject player strategy here
    ExplorationMission(Ship ship) {
        super(ship)
    }

    @Override
    boolean isComplete() {
        return ship.full
    }

    @Override
    PossibleMove getDesiredMove() {
        def currentCellHalite = ship.haliteAtCurrentPosition
        if (currentCellHalite >= minCellAmount || !ship.hasHaliteToMove()) {
            return ship.createPossibleMove(Direction.STILL)
        }
        InfluenceVector influence = ship.influence
        Log.log("Deciding move with influence $influence")
        return possibleCardinalMoves()
        //TODO: I don't like that influence is injected into the possible move, should only be relevant here
                .map({ it.influence = influence; it })
                .filter({ it.halite > currentCellHalite || currentCellHalite == 0 })
                .sort({ PossibleMove left, PossibleMove right -> right.halite.compareTo(left.halite) })
                .filter({ it.ableToMoveOrNavigating })
                .defaultIfEmpty(ship.createPossibleMove(Direction.STILL))
                .blockFirst()
    }

    Flux<PossibleMove> possibleCardinalMoves() {
        return Flux.fromIterable(Direction.ALL_CARDINALS)
                .map({ ship.createPossibleMove(it) })
    }

    @Override
    PossibleMove getAlternateMove() {
        return null
    }

    @Override
    Position getDestination() {
        return null
    }

    @Override
    Mission getNextMission() {
        return new DropOffMission(ship)
    }
}

class NavigationMission extends Mission {
    Position destination

    NavigationMission(Ship ship, Position destination) {
        super(ship)
        this.destination = destination
        ship.destination = destination
    }

    @Override
    boolean isComplete() {
        return ship.position == destination
    }

    @Override
    PossibleMove getDesiredMove() {
        return ship.createPossibleMove(getNavigationDirection())
    }

    Direction getNavigationDirection() {
        Direction direction = Direction.STILL
        if (!ship.hasHaliteToMove()) {
            return direction
        }
        if (destination) {
            int absoluteDistance = ship.calculateDistance(destination)

            if (absoluteDistance == 0) {
                direction = Direction.STILL
            } else {
                direction = getDesiredNavigationDirection()
            }
        }
        return direction
    }

    Direction getDesiredNavigationDirection() {
        int dx = destination.x - ship.position.x
        int dy = destination.y - ship.position.y
        int absDx = Math.abs(dx)
        int absDy = Math.abs(dy)
        int wrapped_dx = ship.game.gameMap.width - absDx
        int wrapped_dy = ship.game.gameMap.height - absDy

        Direction direction = Direction.STILL

        List<Direction> directionList = []

        if (dx > 0) {
            directionList.add(absDx < wrapped_dx ? Direction.EAST : Direction.WEST)
        } else if (dx < 0) {
            directionList.add(absDx < wrapped_dx ? Direction.WEST : Direction.EAST)
        }

        if (dy > 0) {
            directionList.add(absDy < wrapped_dy ? Direction.SOUTH : Direction.NORTH)
        } else if (dy < 0) {
            directionList.add(absDy < wrapped_dy ? Direction.NORTH : Direction.SOUTH)
        }

        if (directionList) {
            return MathFlux.min(Flux.fromIterable(directionList)
                    .map({ ship.createPossibleMove(it) }),
                    { PossibleMove left, PossibleMove right -> left.halite.compareTo(right.halite) })
                    .block().direction
        }
        return direction
    }

    @Override
    PossibleMove getAlternateMove() {
        return null
    }

    @Override
    Mission getNextMission() {
        return new ExplorationMission(ship)
    }
}

class DropOffMission extends NavigationMission {
    DropOffMission(Ship ship) {
        super(ship, ship.game.me.shipyard.position)
    }
}

class HoldMission extends NavigationMission {
    HoldMission(Ship ship, Position destination) {
        super(ship, destination)
    }

    @Override
    boolean isComplete() {
        return false
    }

    @Override
    Mission getNextMission() {
        return this
    }
}