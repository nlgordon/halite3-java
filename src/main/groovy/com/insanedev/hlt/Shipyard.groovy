package com.insanedev.hlt

class Shipyard extends Entity {
    Shipyard(final Player owner, final Position position) {
        super(owner, EntityId.NONE, position)
    }

    Command spawn() {
        return Command.spawnShip()
    }
}
