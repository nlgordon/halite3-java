package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.*
import spock.lang.Ignore
import spock.lang.Unroll

class TestShip extends BaseTestFakeGameEngine {

    Ship ship
    GameMap gameMap

    def setup() {
        initGame(0, 1, 1, 4, 4)
        ship = engine.createShipAtShipyard()
        gameMap = game.gameMap
    }

    def "When a ship is destroyed, it no longer reports active"() {
        when:
        ship.destroy()

        then:
        !ship.active
    }

    def "When a ship is destroyed, it is removed from the game map"() {
        when:
        ship.destroy()

        then:
        gameMap[ship].ship == null
    }

    def "When a ship is destroyed, it can no longer move"() {
        when:
        ship.destroy()
        ship.move(Direction.EAST)

        then:
        thrown(Error)
    }

    def "When a ship is destroyed, it can no longer stay still"() {
        when:
        ship.destroy()
        ship.move(Direction.STILL)

        then:
        thrown(Error)
    }

    def "When a ship is destroyed, it can no longer convert to a dropoff"() {
        when:
        ship.destroy()
        ship.makeDropoff()

        then:
        thrown(Error)
    }

    def "When a ship is destroyed, it can no longer report a desired move"() {
        when:
        ship.destroy()
        ship.getDesiredMove()

        then:
        thrown(Error)
    }

    def "When a ship is ordered to move, it returns an MoveCommand with direction specified"() {
        expect:
        ship.move(direction).direction == direction

        where:
        direction << [Direction.STILL] + Direction.ALL_CARDINALS
    }

    @Ignore
    def "When a ship at 1,1 is ordered to navigate through a ship that doesn't declare a move at 2,1, it stays still"() {
        engine.createShip(2, 1, 0)
        def ship = setupShipForNavigation(0, 2, 1)
        when:
        navigateShips()
        then:
        ship.position == new Position(1,1)
    }

    @Unroll
    def "When a ship at 1,1 is ordered to navigate to #x,#y, it makes that move in 1 turn"() {
        Ship ship = setupShipForNavigation(0, x, y)

        when:
        navigateShips()

        then:
        ship.position == ship.destination

        where:
        x | y
        2 | 1
        1 | 2
        0 | 1
        1 | 0
    }

    @Unroll
    def "When a ship at 1,1 is ordered to navigate to a #x,#y, it makes that move in 2 turns"() {
        def ship = setupShipForNavigation(0, x, y)

        when:
        runTurns(2)

        then:
        ship.position == ship.destination

        where:
        x << [0, 2, 2, 0]
        y << [0, 2, 0, 2]
    }

    @Unroll
    def "When a ship at #startX,#startY is ordered to navigate to #endX,#endY, it makes that move in #turns turns"() {
        def ship = setupShipForNavigation(0, endX, endY)
        engine.updateShipPosition(0, startX, startY)

        when:
        runTurns(turns)

        then:
        ship.position == ship.destination

        where:
        startX | startY | endX | endY || turns
        0      | 0      | 1    | 1    || 2
        0      | 0      | 2    | 2    || 4
        2      | 2      | 0    | 0    || 4
    }

    @Unroll
    def "When a ship at #startX,#startY is ordered to navigate to #endX,#endY, with an obstacle at 1,1 on a 4x4 map, it makes that move in 1 turn"() {
        def ship = setupShipForNavigation(0, endX, endY)
        engine.updateShipPosition(0, startX, startY)
        def obstacle = engine.createShip(1, 1, 0)

        when:
        runTurns(1)

        then:
        ship.position == ship.destination
        obstacle.active
        ship.active

        where:
        startX | startY | endX | endY
        0      | 1      | 3    | 1
        3      | 1      | 0    | 1
        1      | 0      | 1    | 3
        1      | 3      | 1    | 0
    }

    def "When a ship is spawned, it defaults to exploring"() {
        expect:
        getShip(0).status == ShipStatus.EXPLORING
    }

    @Unroll
    def "When a ship is exploring, and at the shipyard, and the cell at #x,#y is the only cell with halite, it will navigate there in 1 turn"() {
        def haliteLocation = new Position(x, y)
        gameMap[haliteLocation].halite = 1000
        when:
        navigateShips()
        then:
        ship.position == haliteLocation

        where:
        x | y
        2 | 1
        0 | 1
        1 | 2
        1 | 0
    }

    def "When a ship is exploring, and at the shipyard, and the cell to the right has the highest halite, it will navigate there in 1 turn"() {
        def haliteLocation = ship.position.directionalOffset(Direction.EAST)
        gameMap[haliteLocation].halite = 1000
        gameMap[ship.position.directionalOffset(Direction.WEST)].halite = 500
        when:
        navigateShips()
        then:
        ship.position == haliteLocation
    }

    def "When a ship is exploring and under its full amount, it will harvest its current location if it is over its minimum"() {
        engine.updateShipPosition(0, 0, 0)
        ship.halite = 500
        def start = new Position(0, 0)
        gameMap[start].halite = 100
        gameMap[start.directionalOffset(Direction.EAST)].halite = 100
        gameMap[start.directionalOffset(Direction.SOUTH)].halite = 100
        ship.minHarvestAmount = 1
        when:
        navigateShips()
        then:
        ship.position == start
    }

    def "When a ship is exploring and the current cell is under the minimum amount, it will harvest its current location if this cell has more than the surrounding cells"() {
        engine.updateShipPosition(0, 0, 0)
        ship.halite = 500
        def start = new Position(0, 0)
        gameMap[start].halite = 110
        gameMap[start.directionalOffset(Direction.EAST)].halite = 100
        gameMap[start.directionalOffset(Direction.SOUTH)].halite = 100
        ship.minHarvestAmount = 200
        when:
        navigateShips()
        then:
        ship.position == start
    }

    def "When a ship is exploring and the current cell is equal to the minimum amount, it will harvest its current location if this cell is equal to than the surrounding cells"() {
        engine.updateShipPosition(0, 0, 0)
        ship.halite = 500
        def start = new Position(0, 0)
        gameMap[start].halite = 100
        gameMap[start.directionalOffset(Direction.EAST)].halite = 100
        gameMap[start.directionalOffset(Direction.SOUTH)].halite = 100
        ship.minHarvestAmount = 25
        when:
        navigateShips()
        then:
        ship.position == start
    }

    def "When a ship is exploring, the current cell and all surrounding cells have zero halite, it will move to a new cell"() {
        engine.updateShipPosition(0, 0, 0)
        ship.halite = 500
        def start = new Position(0, 0)
        when:
        navigateShips()
        then:
        ship.position != start
    }

    def "When a ship is exploring and hits its full amount, it will navigate back to the shipyard"() {
        engine.updateShipPosition(0, 0, 0)
        ship.halite = 500
        def start = new Position(0, 0)
        gameMap[start].halite = 200
        ship.fullAmount = 500
        when:
        navigateShips()
        then:
        ship.destination == player.shipyard.position
    }

    def "When a navigating ship at 0,0 with 0 halite on board, and 100 halite in the map cell, will desire to stay still"() {
        engine.updateShipPosition(0, 0, 0)
        gameMap[new Position(0, 0)].halite = 100
        ship.destination = new Position(1,1)
        when:
        def move = ship.getDesiredMove()
        then:
        move.direction == Direction.STILL
    }

    def "When an exploring ship at 0,0 with 0 halite on board, and 100 halite in the map cell, will desire to stay still"() {
        engine.updateShipPosition(0, 0, 0)
        gameMap[new Position(0, 0)].halite = 100
        gameMap[new Position(1, 0)].halite = 100
        when:
        def move = ship.getDesiredMove()
        then:
        move.direction == Direction.STILL
    }

    def "When a ship moves, it removes itself from the mapcell"() {
        when:
        ship.move(Direction.EAST)

        then:
        !game.gameMap[ship.position].occupied
    }

    def "When a ship moves, it places itself on the target square"() {
        when:
        ship.move(Direction.EAST)

        then:
        game.gameMap[new Position(2, 1)].ship == ship
    }

    def "When a navigating ship reaches it's destination, it switches to exploring on the next updateFrame"() {
        setup:
        def ship = setupShipForNavigation(0, 2, 1)
        when:
        navigateShips()

        then:
        ship.status == ShipStatus.EXPLORING
    }

    def "When a ship status is holding, it will return a stay still move"() {
        ship.status = ShipStatus.HOLDING
        expect:
        ship.getDesiredMove().direction == Direction.STILL
    }

    def "When executing a move, only remove yourself from the mapcell if you are still the ship of record"() {
        def ship2 = engine.createShip(0,0,0)
        game.gameMap[ship].ship = ship2
        when:
        ship.move(Direction.EAST)
        then:
        game.gameMap[ship].ship == ship2
    }

    def "If a ship is destroyed as the result of a move, the destination map cell is cleared out"() {
        def ship2 = engine.createShip(0, 0, 0)
        engine.updateShipPosition(0, 0, 2)
        ship.move(Direction.NORTH)
        ship.destroy()
        gameMap[new Position(0, 1)].halite = 1000
        when:
        navigateShips()
        then:
        ship2.position == ship.position.directionalOffset(Direction.NORTH)
    }
}
