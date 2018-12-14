package com.insanedev

import com.insanedev.hlt.Direction
import com.insanedev.hlt.Log
import com.insanedev.hlt.MoveCommand
import com.insanedev.hlt.PossibleMove
import com.insanedev.hlt.Ship

import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

class Dispatcher {

    List<MoveCommand> executeDesiredMoves(Stream<Ship> activeShips) {
        List<MoveCommand> executedCommands = []
        Tuple2<List<PossibleMove>, List<MoveCommand>> state = new Tuple2<>(collectDesiredMoves(activeShips), executedCommands)
        state = executeEasyMoves(state)

        executedCommands.addAll(state.second)

        return executedCommands + executeHardMoves(state.first)
    }

    private List<PossibleMove> collectDesiredMoves(Stream<Ship> activeShips) {
        return activeShips
                .map({ it.getDesiredMove() })
                .collect(Collectors.toList())
    }

    private Tuple2<List<PossibleMove>, List<MoveCommand>> executeEasyMoves(Tuple2<List<PossibleMove>, List<MoveCommand>> state) {
        executeAbleMoves(state.first)
        if (!hasExecutedMoves(state.first)) {
            return state
        }
        List<MoveCommand> easyExecutedMoves = state.second + getExecutedMoves(state.first)

        List<PossibleMove> hardMoves = getUnexecutedMoves(state.first)

        Tuple2<List<PossibleMove>, List<MoveCommand>> newState = new Tuple2<>(hardMoves, easyExecutedMoves)

        return executeEasyMoves(newState)
    }

    private Collection getUnexecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ !it.executed }).collect()
    }

    private boolean hasExecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ it.executed }).count()
    }

    private void executeAbleMoves(List<PossibleMove> first) {
        Log.log("Executing moves: " + first.collect({ it.toString() }).join(", "))
        first.stream().forEach({ it.executeIfAble() })
    }

    private Collection getExecutedMoves(List<PossibleMove> first) {
        return first.stream().filter({ it.executed }).map({ it.command }).collect()
    }

    private List<MoveCommand> executeHardMoves(List<PossibleMove> desiredMoves) {
        Map<Ship, PossibleMove> coordinatingMoves = possibleMovesToMapByShip(desiredMoves)
        return coordinatingMoves.values().collect().stream()
                .filter({ coordinatingMoves.containsKey(it.ship) })
                .flatMap({
            List<PossibleMove> chainOfMoves = chainRequiredMoves([], coordinatingMoves, it, it.ship)
            Log.log("Hard move: ${it}")
            if (chainOfMoves) {
                Log.log("Hard move chain: " + chainOfMoves.collect({ it.toString() }).join(", "))
                chainOfMoves.stream().forEach({ coordinatingMoves.remove(it.ship) })
                return chainOfMoves.stream().map({ it.executeMove() })
            } else {
                if (FeatureFlags.getFlagStatus("NO_ALTERNATES") && it.mapCell.isOccupiedFriendly(this)) {
                    Log.log("No alternate allowed for ${it.ship.id}")
                    return Stream.of(it.ship.createPossibleMove(Direction.STILL).executeMove())
                }
                Log.log("Finding alternate route for ${it.ship.id}")
                return Stream.of(it.getAlternateRoute().executeMove())
            }
        }).collect()
    }

    private List<PossibleMove> chainRequiredMoves(List<PossibleMove> movesSoFar, Map<Ship, PossibleMove> poolOfMoves, PossibleMove currentMove, Ship startingShip) {
        def blockingShip = currentMove.mapCell.ship
        boolean blockingShipAlreadySeen = movesSoFar.stream().anyMatch({ it.ship == blockingShip })
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

    private Map<Ship, PossibleMove> possibleMovesToMapByShip(List<PossibleMove> desiredMoves) {
        return desiredMoves.stream()
                .collect(Collectors.toMap({ it.ship }, Function.identity()))
    }
}
