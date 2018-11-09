package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Ship extends Entity {
    int halite
    boolean destroyed

    Ship(final PlayerId owner, final EntityId id, final Position position, final int halite) {
        super(owner, id, position)
        this.halite = halite
    }

    Ship(final Player player, final EntityId id, final Position position, final int halite) {
        super(player, id, position)
        this.halite = halite
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
        // Need to remove from map cell as well
    }

    void assertActive() {
        assert !destroyed
    }
}
