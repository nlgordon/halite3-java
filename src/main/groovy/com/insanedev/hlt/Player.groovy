package com.insanedev.hlt

import com.insanedev.hlt.engine.PlayerUpdate

class Player {
    final PlayerId id
    final Shipyard shipyard
    int halite
    final Map<EntityId, Ship> ships = [:]
    final Map<EntityId, Dropoff> dropoffs = [:]

    private Player(final PlayerId id, final Shipyard shipyard) {
        this.id = id
        this.shipyard = shipyard
    }

    static Player create(int playerId, int shipyard_x, int shipyard_y) {
        new Player(new PlayerId(playerId), new Shipyard(new PlayerId(playerId), new Position(shipyard_x, shipyard_y)))
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
                .findAll {it.active}
                .collect({it.id})
        destroyedShips.removeAll(updatedShips)

        if (destroyedShips) {
            Log.debug("Ships were destroyed: $destroyedShips")
        }

        destroyedShips.forEach {
            ships[it].destroy()
        }
    }
}
