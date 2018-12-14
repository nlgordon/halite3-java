package com.insanedev

import com.insanedev.hlt.*
import reactor.core.publisher.Flux
import reactor.math.MathFlux

/**
 * TODO:
 * Ship navigation mgmt system
 * There is a need board responsible for finding assignments for ships
 * Each ship mission is potentially tied to an assignment
 * Assignments include areas of interest, shipyards/dropoffs
 * Areas re-assign a full ship to a shipyard when full
 * Shipyards re-assign a ship based on the need board once emptied
 * Create interfaces describing different types of need and a way to indicate the need is fullfilled
 * Shipyards and areas have dispatchers who take control of their owned ships to coordinate movement
 * Shipyards create entrance and exit lanes that are assigned to a ship to manage entrance and exit
 * Areas analyze their area and level of depletion to determine how many ships they need
 * Advanced logic in an area would look at cross section of harvesting to maximize efficiency
 * Implement control of ship movements as dispatchers that coordinate incoming movement and graceful exit
 * Dispatcher might be forward planning for an entire path, or only a current turn planner
 * Shipyard dispatcher would likely be forward planning to coordinate lots of ships moving in and out of the single cell
 * Area dispatcher would likely look at just the next best cell
 * Dispatchers would be responsible for negotiating when ships want to move across each other
 * Would also have one dispatcher for unguided exploration/assignment
 */

abstract class Mission {
    Ship ship
    PlayerStrategy strategy

    Mission(Ship ship, PlayerStrategy strategy) {
        this.ship = ship
        this.strategy = strategy
    }

    abstract boolean isComplete()

    abstract PossibleMove getDesiredMove()

    abstract PossibleMove getAlternateMove()

    abstract Position getDestination()

    abstract Mission getNextMission()
}

class ExplorationMission extends Mission {
    int minCellAmount = Configurables.MIN_CELL_AMOUNT

    ExplorationMission(Ship ship, PlayerStrategy strategy) {
        super(ship, strategy)
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

        InfluenceVector influence

        if (ship.currentCell.area) {
            influence = ship.currentCell.area.getInnerAreaInfluence(ship)
        } else {
            influence = strategy.getExplorationInfluence(ship)
        }

        Log.log("Deciding move with influence $influence")
        return possibleCardinalMoves()
        //TODO: I don't like that influence is injected into the possible move, should only be relevant here to filter cardinals
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
        return new DropOffMission(ship, strategy)
    }
}

class NavigationMission extends Mission {
    Position destination

    NavigationMission(Ship ship, Position destination, PlayerStrategy strategy) {
        super(ship, strategy)
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
        return new ExplorationMission(ship, strategy)
    }
}

class DropOffMission extends NavigationMission {
    DropOffMission(Ship ship, PlayerStrategy strategy) {
        super(ship, ship.game.me.shipyard.position, strategy)
    }
}

class HoldMission extends NavigationMission {
    HoldMission(Ship ship, Position destination, PlayerStrategy strategy) {
        super(ship, destination, strategy)
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