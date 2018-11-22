package com.insanedev.hlt

import com.insanedev.hlt.engine.PlayerUpdate
import groovy.transform.EqualsAndHashCode

import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

@EqualsAndHashCode(includes = "id")
class Player {
    final PlayerId id
    Shipyard shipyard
    int halite
    final Map<EntityId, Ship> ships = [:]
    final Map<EntityId, Dropoff> dropoffs = [:]

    private Player(final PlayerId id) {
        this.id = id
    }

    Ship getShip(int id) {
        return ships[new EntityId(id)]
    }

    static Player create(int playerId, int shipyard_x, int shipyard_y) {
        Player player = new Player(new PlayerId(playerId))
        def shipyard = new Shipyard(player, new Position(shipyard_x, shipyard_y))
        player.shipyard = shipyard
        return player
    }

    void applyUpdate(PlayerUpdate update) {
        Log.debug("Updating player $id with ${update.numberOfShips} ships, ${update.numberOfDropoffs} dropoffs, and ${update.halite} halite")
        halite = update.halite

        List<EntityId> updatedShips = update.shipUpdates.collect({
            it.apply(this)
        })

        markShipsDestroyed(updatedShips)

        Log.debug("Player Ships: ${ships.keySet()}")

        update.dropoffUpdates.each({
            it.buildDropoff(this)
        })
    }

    void markShipsDestroyed(List<EntityId> updatedShips) {
        Set<EntityId> destroyedShips = ships.values()
                .findAll { it.active }
                .collect({ it.id })
        destroyedShips.removeAll(updatedShips)

        if (destroyedShips) {
            Log.debug("Ships were destroyed: $destroyedShips")
        }

        destroyedShips.forEach {
            ships[it].destroy()
        }
    }

    List<MoveCommand> navigateShips() {
        //logActiveShips()
        List<MoveCommand> executedCommands = []
        Tuple2<List<PossibleMove>, List<MoveCommand>> state = new Tuple2<>(collectDesiredMoves(), executedCommands)
        state = executeEasyMoves(state)

        executedCommands.addAll(state.second)

        return executedCommands + executeHardMoves(state.first)
    }

    Tuple2<List<PossibleMove>, List<MoveCommand>> executeEasyMoves(Tuple2<List<PossibleMove>, List<MoveCommand>> state) {
        executeAbleMoves(state.first)
        if (!hasExecutedMoves(state.first)) {
            return state
        }
        List<MoveCommand> easyExecutedMoves = state.second + getExecutedMoves(state.first)

        List<PossibleMove> hardMoves = getUnexecutedMoves(state.first)

        Tuple2<List<PossibleMove>, List<MoveCommand>> newState = new Tuple2<>(hardMoves, easyExecutedMoves)

        return executeEasyMoves(newState)
    }

    Collection getUnexecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ !it.executed }).collect()
    }

    boolean hasExecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ it.executed }).count()
    }

    void executeAbleMoves(List<PossibleMove> first) {
        Log.log("Executing moves: " + first.collect({it.toString()}).join(", "))
        first.stream().forEach({ it.executeIfAble() })
    }

    Collection getExecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ it.executed }).map({ it.command }).collect()
    }

    List<PossibleMove> collectDesiredMoves() {
        return getActiveShips()
                .map({ it.getDesiredMove() })
                .collect(Collectors.toList())
    }

    Stream<Ship> getActiveShips() {
        return ships.values().stream()
                .filter({ it.active })
    }

    List<MoveCommand> executeHardMoves(List<PossibleMove> desiredMoves) {
        Map<Ship, PossibleMove> coordinatingMoves = possibleMovesToMapByShip(desiredMoves)
        return coordinatingMoves.values().collect().stream()
                .filter({ coordinatingMoves.containsKey(it.ship) }).flatMap({
            List<PossibleMove> chainOfMoves = chainRequiredMoves([], coordinatingMoves, it, it.ship)
            Log.log("Hard move: ${it}")
            if (chainOfMoves) {
                Log.log("Hard move chain: " + chainOfMoves.collect({it.toString()}).join(", "))
                chainOfMoves.stream().forEach({coordinatingMoves.remove(it.ship)})
                return chainOfMoves.stream().map({it.executeMove()})
            } else {
                Log.log("Finding alternate route for ${it.ship.id}")
                return Stream.of(it.getAlternateRoute().executeMove())
            }
        }).collect()
    }

    List<PossibleMove> chainRequiredMoves(List<PossibleMove> movesSoFar, Map<Ship, PossibleMove> poolOfMoves, PossibleMove currentMove, Ship startingShip) {
        def blockingShip = currentMove.mapCell.ship
        boolean blockingShipAlreadySeen = movesSoFar.stream().anyMatch({it.ship == blockingShip})
        if (!poolOfMoves.containsKey(blockingShip)) {
            // Reached a dead end
            return []
        } else if (startingShip != blockingShip) {
            if (blockingShipAlreadySeen && startingShip != blockingShip) {
                return []
            }
            def blockingMove = poolOfMoves[blockingShip]
            return chainRequiredMoves(movesSoFar + currentMove, poolOfMoves, blockingMove, startingShip)
        }
        return movesSoFar + currentMove
    }

    Map<Ship, PossibleMove> possibleMovesToMapByShip(List<PossibleMove> desiredMoves) {
        return desiredMoves.stream()
                .collect(Collectors.toMap({ it.ship }, Function.identity()))
    }

    void logActiveShips() {
        Log.log("Active ships:")
        getActiveShips().forEach({
            def cell = it.game.gameMap[it]
            Log.log("$it $cell") })
    }
}
