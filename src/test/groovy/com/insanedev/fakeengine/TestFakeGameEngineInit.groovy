package com.insanedev.fakeengine

import com.insanedev.hlt.Game

class TestFakeGameEngineInit extends BaseTestFakeGameEngine {

    def setup() {
        initGame(0, 1, 1, 3, 3)
    }

    def "Engine Can initialize a game with one player"() {
        expect:
        engine.init().me == player
    }

    def "Engine creates a map with 3x3 size"() {
        when:
        Game game = engine.init()

        then:
        game.gameMap.height == 3
        game.gameMap.width == 3
    }

    def "Engine can create a map with 4x1 size"() {
        setup:
        FakeGameEngine engine = new FakeGameEngine(this.player, 4, 1)

        when:
        Game game = engine.init()

        then:
        game.gameMap.width == 4
        game.gameMap.height == 1
    }

    def "Engine adds player to the list of players"() {
        expect:
        engine.init().players.contains(player)
    }
}
