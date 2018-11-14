package com.insanedev.hlt.engine

import com.insanedev.hlt.Dropoff
import com.insanedev.hlt.EntityId
import com.insanedev.hlt.Game
import com.insanedev.hlt.Player
import com.insanedev.hlt.Position

class DropoffUpdate extends GameUpdate {
    final EntityId id
    final Position position

    DropoffUpdate(Game game, final EntityId id, final Position position) {
        super(game)
        this.id = id
        this.position = position
    }

    void buildDropoff(Player player) {
        if (!player.dropoffs.containsKey(id)) {
            Dropoff dropoff = new Dropoff(player, id, position)
            player.dropoffs[id] = dropoff
            game.gameMap.at(dropoff).structure = dropoff
        }
    }
}
