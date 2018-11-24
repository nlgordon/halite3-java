package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine

class TestShipyard extends BaseTestFakeGameEngine {
    def setup() {
        initGameWithMultiplePlayers(2, 2,  14, 14, 16, 16)
    }

    def "When the player updates its shipyards they do not register as occupied if it is occupied by an enemy player"() {
        engine.createShipForPlayer(1, 2, 2, 0)
        when:
        player.updateDropoffs()

        then:
        !game.gameMap[player.shipyard.position].occupied
    }
}
