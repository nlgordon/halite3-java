package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Ship extends Entity {
    int halite
    boolean destroyed
    final Game game

    Ship(Game game, final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
        this.game = game
    }

    boolean isFull() {
        return halite >= Constants.MAX_HALITE
    }

    Command makeDropoff() {
        assertActive()
        return Command.transformShipIntoDropoffSite(id)
    }

    Command move(final Direction direction) {
        assertActive()
        return Command.move(id, direction)
    }

    Command stayStill() {
        assertActive()
        return Command.move(id, Direction.STILL)
    }

    void destroy() {
        destroyed = true
        game.gameMap[this].ship = null
    }

    boolean isActive() {
        return !destroyed
    }

    void assertActive() {
        assert !destroyed
    }
}
