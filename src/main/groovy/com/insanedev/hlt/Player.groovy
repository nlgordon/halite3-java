package com.insanedev.hlt

import com.insanedev.hlt.engine.PlayerUpdate
import groovy.transform.EqualsAndHashCode

import java.util.function.Function
import java.util.stream.Collectors

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
        List<PossibleMove> desiredMoves = collectDesiredMoves()
        Map<Boolean, List<PossibleMove>> groupedMoves = desiredMoves.groupBy({ it.ableToMove })

        List<PossibleMove> easyMoves = groupedMoves.getOrDefault(true, [])
        // Now process hard moves to see if any are possible now
        List<MoveCommand> executedCommands = executeMoves(easyMoves)
        return executedCommands + executeHardMoves(groupedMoves)
    }

    private List<PossibleMove> collectDesiredMoves() {
        ships.values().stream()
                .map({ it.getDesiredMove() })
                .collect(Collectors.toList())
    }

    private Collection executeMoves(List<PossibleMove> moves) {
        moves.stream().map({ it.executeMove() }).collect()
    }


    List<MoveCommand> executeHardMoves(Map<Boolean, List<PossibleMove>> desiredMoves) {
        if (desiredMoves[false]) {
            Map<Ship, PossibleMove> coordinatingMoves = desiredMoves[false].stream()
                    .collect(Collectors.toMap({ it.ship }, Function.identity()))
            return coordinatingMoves.values().collect().stream()
                    .filter({ coordinatingMoves.containsKey(it.ship) }).flatMap({
                def blockingShip = it.mapCell.ship
                if (coordinatingMoves.containsKey(blockingShip)) {
                    // Recursive dig through map until we form a loop, which then results in execution, or else find alternate routes
                    def blockingMove = coordinatingMoves[blockingShip]

                    if (blockingMove.mapCell.ship == it.ship) {
                        // We formed a loop of two! Execute the moves!
                        coordinatingMoves.remove(blockingShip)
                        return [it.executeMove(), blockingMove.executeMove()].stream()
                    }
                } else {
                    return [it.getAlternateRoute().executeMove()].stream()
                }
            }).collect()
        }
        return []
    }

    List<MoveCommand> executeEasyMoves(Map<Boolean, List<PossibleMove>> desiredMoves) {
        if (desiredMoves[true]) {
            return desiredMoves[true].stream().map({ it.executeMove() }).collect()
        }
        return []
    }
}
