package com.insanedev.hlt.engine

import com.insanedev.hlt.EntityId
import com.insanedev.hlt.Game
import com.insanedev.hlt.Log
import com.insanedev.hlt.Player
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship

class ShipUpdate extends GameUpdate {
    final EntityId id
    final Position position
    final int halite

    ShipUpdate(Game game, final EntityId id, final Position position, final int halite) {
        super(game)
        this.id = id
        this.position = position
        this.halite = halite
    }

    EntityId apply(Player player) {
        Ship ship = player.ships[id]
        if (ship) {
            game.gameMap.at(ship).ship = null
            ship.position = position
            ship.halite = halite
        } else {
            ship = player.ships[id] = new Ship(player, id, position, halite)
        }
        game.gameMap.at(ship).markUnsafe(ship)

        Log.debug("Updating player ${player.id} ship ${ship.id} at ${ship.position.x} ${ship.position.y} with halite ${ship.halite}")

        return id
    }
}
