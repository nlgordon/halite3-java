package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

import java.util.stream.IntStream
import java.util.stream.Stream

class TestShipComplexNavigation extends BaseTestFakeGameEngine {

    def setup() {
        initGame(0, 2, 2, 16, 16)
    }

    def "Map is 16x16"() {
        expect:
        game.gameMap.width == 16
        game.gameMap.height == 16
    }

    def "Two ships, one at 0,0 and one at 3,3 navigating to each others start point will arrive in 3 turns"() {
        def shipOneStart = new Position(0, 0)
        def shipTwoStart = new Position(3, 0)
        def shipOne = engine.createShip(shipOneStart, 0)
        def shipTwo = engine.createShip(shipTwoStart, 0)

        shipOne.destination = shipTwoStart
        shipTwo.destination = shipOneStart

        when:
        IntStream.range(0, 3).forEach({
            println("1: ${shipOne.position} 2: ${shipTwo.position}")
            engine.endTurn(Stream.of(shipOne, shipTwo).collect({ it.navigate() }).collect())
            engine.updateFrame()
        })

        then:
        shipOne.position == shipTwoStart
        shipTwo.position == shipOneStart
    }
}
