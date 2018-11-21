package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship

class TestPlayer extends BaseTestFakeGameEngine {
    Ship ship

    def setup() {
        initGame(0, 1, 1, 3, 3)
        ship = engine.createShipAtShipyard()
    }

    def "Player will not attempt navigation on destroyed ships" () {
        ship.destination = new Position(2,2)
        ship.destroy()
        when:
        navigateShips()
        then:
        ship.position == new Position(1,1)
    }
}
