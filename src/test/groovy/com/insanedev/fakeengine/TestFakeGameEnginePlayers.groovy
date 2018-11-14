package com.insanedev.fakeengine

import com.insanedev.hlt.Game
import com.insanedev.hlt.Player
import spock.lang.Specification

class TestFakeGameEnginePlayers extends Specification {
    private FakeGameEngine engine

    def setup() {
        engine = new FakeGameEngine(Player.create(0, 0, 0))
    }

    def "On first call to updateFrame, the player is set to 5000 halite"() {
        setup:
        Game game = engine.init()

        when:
        engine.updateFrame()

        then:
        game.me.halite == 5000
    }

    def "Players are created without any ships"() {
        expect:
        engine.init().me.ships.size() == 0
    }
}
