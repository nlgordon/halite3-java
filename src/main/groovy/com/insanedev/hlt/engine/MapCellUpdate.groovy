package com.insanedev.hlt.engine

import com.insanedev.hlt.Game
import com.insanedev.hlt.Position

class MapCellUpdate extends GameUpdate {
    final Position position
    final int halite

    MapCellUpdate(Game game, final Position position, final int halite) {
        super(game)
        this.position = position
        this.halite = halite
    }

    void apply() {
        game.gameMap.at(position).halite = halite
    }
}
