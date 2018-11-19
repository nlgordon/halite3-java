package com.insanedev.hlt.engine

import com.insanedev.hlt.EntityId
import com.insanedev.hlt.Game
import com.insanedev.hlt.Log
import com.insanedev.hlt.Player
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes="id")
class ShipUpdate extends GameUpdate {
    final EntityId id
    Position position
    int halite

    ShipUpdate(Game game, final EntityId id, final Position position, final int halite) {
        super(game)
        this.id = id
        this.position = position
        this.halite = halite
    }

    EntityId apply(Player player) {
        Ship ship = player.ships[id]
        if (ship) {
            ship.update(position, halite)
        } else {
            ship = player.ships[id] = new Ship(game, player, id, position, halite)
        }

        Log.debug("Updating player ${player.id} ship ${ship.id} at ${ship.position.x} ${ship.position.y} with halite ${ship.halite}")

        return id
    }
}
