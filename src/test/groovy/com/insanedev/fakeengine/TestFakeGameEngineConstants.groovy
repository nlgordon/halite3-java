package com.insanedev.fakeengine

import com.insanedev.hlt.Constants
import com.insanedev.hlt.Player
import spock.lang.Specification

class TestFakeGameEngineConstants extends Specification {
    private FakeGameEngine engine

    def setup() {
        engine = new FakeGameEngine(Player.create(0, 0, 0))
    }

    def "Engine initalizes MAX_HALITE"() {
        when:
        engine.init()

        then:
        Constants.MAX_HALITE == 1000
    }

    def "Engine initalizes SHIP_COST"() {
        when:
        engine.init()

        then:
        Constants.SHIP_COST == 1000
    }
}
