package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.GameMap
import com.insanedev.hlt.Position

class TestGameMap extends BaseTestFakeGameEngine {
    GameMap gameMap
    def setup() {
        initGameWithMultiplePlayers(2, 2, 14, 14, 16, 16)
        gameMap = game.gameMap
    }
    def "A map cell at 1,1 will have a north field pointing to position 1, 0"() {
        expect:
        gameMap.at(1,1).north.position == new Position(1, 0)
    }

    def "A map cell at 1,1 will have a south field pointing to position 1, 2"() {
        expect:
        gameMap.at(1,1).south.position == new Position(1, 2)
    }

    def "A map cell at 1,1 will have a east field pointing to position 2, 1"() {
        expect:
        gameMap.at(1,1).east.position == new Position(2, 1)
    }

    def "A map cell at 1,1 will have a west field pointing to position 0, 1"() {
        expect:
        gameMap.at(1,1).west.position == new Position(0, 1)
    }

    def "A mapcell at 1,1 with an opponent ship at 1,0 will report as near opponent"() {
        engine.createShipForPlayer(1, 1, 0, 0)
        expect:
        gameMap.at(1,1).isNearOpponent(player)
    }

    def "A mapcell at 1,1 with an opponent ship at 1,2 will report as near opponent"() {
        engine.createShipForPlayer(1, 1, 2, 0)
        expect:
        gameMap.at(1,1).isNearOpponent(player)
    }

    def "A mapcell at 1,1 with an opponent ship at 0,1 will report as near opponent"() {
        engine.createShipForPlayer(1, 0, 1, 0)
        expect:
        gameMap.at(1,1).isNearOpponent(player)
    }

    def "A mapcell at 1,1 with an opponent ship at 2,1 will report as near opponent"() {
        engine.createShipForPlayer(1, 2, 1, 0)
        expect:
        gameMap.at(1,1).isNearOpponent(player)
    }
}
