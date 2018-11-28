package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

class TestShipyard extends BaseTestFakeGameEngine {
    def setup() {
        initGameWithMultiplePlayers(2, 2, 14, 14, 16, 16)
    }

    def "When the player updates its shipyards they do not register as occupied if it is occupied by an enemy player"() {
        engine.createShipForPlayer(1, 2, 2, 0)
        when:
        player.updateDropoffs()

        then:
        !game.gameMap[player.shipyard.position].occupied
    }

    def "Once the shipyard is no longer occupied by an opponent, and then by the player, it registers as occupied"() {
        def myShip = engine.createShip(1,1, 0)
        def opponentShip = engine.createShipForPlayer(1, 2, 2, 0)
        player.updateDropoffs()
        engine.updateShipPosition(opponentShip, new Position(3,3))
        engine.updateShipPosition(myShip, player.shipyard.position)
        when:
        player.updateDropoffs()

        then:
        game.gameMap[player.shipyard.position].occupied

    }
}
