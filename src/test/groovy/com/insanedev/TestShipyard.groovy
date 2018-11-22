package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine

class TestShipyard extends BaseTestFakeGameEngine {
    def setup() {
        initGameWithMultiplePlayers(2, 2,  14, 14, 16, 16)
    }

    def "When the shipyard is occupied by an enemy player, it does not register as occupied"() {
        when:
        engine.createShipForPlayer(1, 2, 2, 0)

        then:
        !game.gameMap[player.shipyard.position].occupied
    }
}
