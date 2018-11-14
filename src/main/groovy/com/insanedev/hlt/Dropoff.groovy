package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Dropoff extends Entity {
    Dropoff(final Player owner, final EntityId id, final Position position) {
        super(owner, id, position)
    }
}
