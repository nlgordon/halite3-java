package com.insanedev.fakeengine


import com.insanedev.hlt.Game
import com.insanedev.hlt.Player
import spock.lang.Specification

class TestFakeGameEngineInit extends Specification {
    private Player player
    private FakeGameEngine engine

    def setup() {
        player = Player.create(0, 0, 0)
        engine = new FakeGameEngine(this.player)
    }

    def "Engine Can initialize a game with one player"() {
        expect:
        engine.init().me == player
    }

    def "Engine creates a map with 1x1 size"() {
        when:
        Game game = engine.init()

        then:
        game.gameMap.height == 1
        game.gameMap.width == 1
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
