package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class PlayerId {
    final int id

    PlayerId(int id) {
        this.id = id
    }

    @Override
    String toString() {
        return String.valueOf(id)
    }
}
