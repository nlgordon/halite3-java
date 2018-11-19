package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.*
import spock.lang.Unroll

class TestShip extends BaseTestFakeGameEngine {

    Ship ship
    GameMap gameMap

    def setup() {
        initGame(0, 1, 1, 3, 3)
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

    def "When a ship is ordered to move, it returns an MoveCommand with direction specified"() {
        expect:
        ship.move(direction).direction == direction

        where:
        direction << [Direction.STILL] + Direction.ALL_CARDINALS
    }

    def "When a ship at 1,1 is ordered to navigate through a friendly ship at 2,1, it stays still"() {
        engine.createShip(2, 1, 0)
        def ship = setupShipForNavigation(0, 2, 1)
        when:
        navigateShip(ship)
        then:
        ship.position == new Position(1,1)
    }

    @Unroll
    def "When a ship at 1,1 is ordered to navigate to 2,1, it makes that move in 1 turn"() {
        Ship ship = setupShipForNavigation(0, x, y)

        when:
        navigateShip(ship)

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
        (1..2).forEach({
            navigateShip(ship)
        })

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
        (0..<turns).stream().forEach({
            navigateShip(ship)
        })

        then:
        ship.position == ship.destination

        where:
        startX | startY | endX | endY || turns
        0      | 0      | 1    | 1    || 2
        0      | 0      | 2    | 2    || 4
        2      | 2      | 0    | 0    || 4
    }

    def "When a ship at 0,1 is ordered to navigate to 2,1, with an obstacle at 1,1 it makes that move in 4 turns"() {
        def ship = setupShipForNavigation(0, 2, 1)
        engine.updateShipPosition(0, 0, 1)
        def obstacle = engine.createShip(1, 1, 0)

        when:
        (0..<4).stream().forEach({
            navigateShip(ship)
        })

        then:
        ship.position == ship.destination
        obstacle.active
        ship.active
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
        navigateShip(ship)
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
        navigateShip(ship)
        then:
        ship.position == haliteLocation
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
        navigateShip(ship)

        then:
        ship.status == ShipStatus.EXPLORING
    }


}
