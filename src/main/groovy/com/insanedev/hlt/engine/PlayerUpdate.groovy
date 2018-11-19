package com.insanedev.hlt.engine

import com.insanedev.hlt.Game
import com.insanedev.hlt.PlayerId

class PlayerUpdate extends GameUpdate {
    final PlayerId id
    int halite
    final List<ShipUpdate> shipUpdates
    final List<DropoffUpdate> dropoffUpdates

    PlayerUpdate(Game game, final PlayerId id, final int halite, List<ShipUpdate> shipUpdates, List<DropoffUpdate> dropoffUpdates) {
        super(game)
        this.id = id
        this.halite = halite
        this.shipUpdates = shipUpdates
        this.dropoffUpdates = dropoffUpdates
    }

    int getNumberOfShips() {
        return shipUpdates.size()
    }

    int getNumberOfDropoffs() {
        return dropoffUpdates.size()
    }
}
