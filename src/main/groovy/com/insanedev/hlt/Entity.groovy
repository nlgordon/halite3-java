package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Entity {
    final PlayerId owner
    final Player player
    final EntityId id
    Position position

    Entity(final PlayerId owner, final EntityId id, final Position position) {
        this.owner = owner
        this.id = id
        this.position = position
    }

    Entity(final Player player, final EntityId id, final Position position) {
        this.owner = player.id
        this.player = player
        this.id = id
        this.position = position
    }
}
