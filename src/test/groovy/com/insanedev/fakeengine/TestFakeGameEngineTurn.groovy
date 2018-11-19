package com.insanedev.fakeengine

class TestFakeGameEngineTurn extends BaseTestFakeGameEngine {

    def setup() {
        initGame(0, 1, 1, 3, 3)
        spawnShip()
    }

    def "Commands are not repeated on subsequent calls to updateFrame"() {
        when:
        engine.updateFrame()

        then:
        player.halite == 4000
    }
}
