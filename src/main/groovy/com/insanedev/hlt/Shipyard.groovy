package com.insanedev.hlt

class Shipyard extends Entity {
    Game game
    Shipyard(final Player owner, final Position position) {
        super(owner, EntityId.NONE, position)
    }

    Command spawn() {
        return Command.spawnShip()
    }
}
