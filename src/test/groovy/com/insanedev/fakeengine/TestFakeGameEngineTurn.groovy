package com.insanedev.fakeengine

class TestFakeGameEngineTurn extends BaseTestFakeGameEngine {

    def setup() {
        spawnShip()
    }

    def "Commands are not repeated on subsequent calls to updateFrame"() {
        when:
        engine.updateFrame()

        then:
        player.halite == 4000
    }
}
