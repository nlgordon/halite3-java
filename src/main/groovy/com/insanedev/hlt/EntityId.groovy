package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class EntityId {
    static final EntityId NONE = new EntityId(-1)

    final int id

    EntityId(int id) {
        this.id = id
    }

    @Override
    String toString() {
        return String.valueOf(id)
    }
}
