package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Direction
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship
import com.insanedev.hlt.ShipStatus
import spock.lang.Unroll

import java.util.stream.IntStream

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

    def "Two ships, one at 0,0 and one at 1,1 navigating to 1,0 will have only one arrive in 1 turn and neither will be destroyed"() {
        def destination = new Position(1, 0)
        def shipOne = engine.createShip(0, 0, 0)
        def shipTwo = engine.createShip(1,1, 0)

        shipOne.destination = destination
        shipTwo.destination = destination

        when:
        runTurns(1)

        then:
        shipOne.active
        shipTwo.active
        shipOne.position == destination || shipTwo.position == destination
    }

    def "Two ships, one at 0,0 and one at 1,1 navigating to an occupied 1,0 that is exploring will have only one arrive in 1 turn and none will be destroyed"() {
        def destination = new Position(1, 0)
        def shipOne = engine.createShip(0, 0, 0)
        def shipTwo = engine.createShip(1,1, 0)
        def shipThree = engine.createShip(1, 0, 0)
        def haliteLocation = new Position(2, 0)
        game.gameMap[haliteLocation].halite = 1000

        shipOne.destination = destination
        shipTwo.destination = destination

        shipThree.destination = haliteLocation

        when:
        runTurns(1)

        then:
        shipOne.active
        shipTwo.active
        shipOne.position == destination || shipTwo.position == destination
    }

    def "Four ships in a sqaure at 2,2, navigating to each others start point will arrive in 1 turn"() {
        def ship1Start = new Position(2, 3)
        def ship2Start = new Position(3, 3)
        def ship3Start = new Position(3, 4)
        def ship4Start = new Position(2, 4)
        def ship1 = engine.createShip(ship1Start, 0)
        def ship2 = engine.createShip(ship2Start, 0)
        def ship3 = engine.createShip(ship3Start, 0)
        def ship4 = engine.createShip(ship4Start, 0)

        ship1.destination = ship2Start
        ship2.destination = ship3Start
        ship3.destination = ship4Start
        ship4.destination = ship1Start

        when:
        runTurns(1)

        then:
        ship1.position == ship2Start
        ship2.position == ship3Start
        ship3.position == ship4Start
        ship4.position == ship1Start
    }

    def "When a ship at 0,1 is ordered to navigate to 2,1, with an obstacle at 1,1 it makes that move in 4 turns"() {
        def ship = engine.createShip(0, 1, 0)
        setupShipForNavigation(ship.id.id, 2, 1)
        def obstacle = engine.createShip(1, 1, 0)
        obstacle.status = ShipStatus.HOLDING

        when:
        runTurns(4)

        then:
        ship.position == ship.destination
        obstacle.active
        ship.active
    }

    @Unroll
    def "When a horizontal line of #count ships at 2,2 want to move east, they move together"() {
        def destination = new Position(2 + count + 2, 2)
        List<Position> startPositions = IntStream.range(0, count)
                .mapToObj({ new Position(2 + it, 2) }).collect()
        List<Position> endPositions = startPositions.stream().map({ it.directionalOffset(Direction.EAST) }).collect()

        List<Ship> ships = startPositions.stream()
                .map({
            def ship = engine.createShip(it, 0)
            ship.destination = destination
            return ship
        }).collect()

        when:
        runTurns(1)

        then:
        IntStream.range(0, count).forEach({ assert ships[it].position == endPositions[it] })

        where:
        count << [2, 3, 4]
    }
}
