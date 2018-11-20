package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

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
        runTurns(3)

        then:
        shipOne.position == shipTwoStart
        shipTwo.position == shipOneStart
    }

    def "When a ship at 0,1 is ordered to navigate to 2,1, with an obstacle at 1,1 it makes that move in 4 turns"() {
        def ship = engine.createShip(0, 1, 0)
        setupShipForNavigation(ship.id.id, 2, 1)
        def obstacle = engine.createShip(1, 1, 0)

        when:
        runTurns(4)

        then:
        ship.position == ship.destination
        obstacle.active
        ship.active
    }

    def "When a horizontal line of 2 ships at 2,2 want to move east, they move together"() {
        def shipOneStart = new Position(2, 2)
        def shipTwoStart = new Position(3, 2)
        def shipOne = engine.createShip(shipOneStart, 0)
        def shipTwo = engine.createShip(shipTwoStart, 0)
        def destination = new Position(6,2)

        shipOne.destination = destination
        shipTwo.destination = destination

        when:
        runTurns(1)

        then:
        shipOne.position == new Position(3, 2)
        shipTwo.position == new Position(4,2)
    }
}
