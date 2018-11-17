package com.insanedev.fakeengine

import com.insanedev.hlt.*

class TestFakeGameEngineMovement extends BaseTestFakeGameEngine {
    Ship ship

    def setup() {
        ship = engine.createShip(0, 0, 0)
    }

    def "Ordering a ship to move one unit East from 0,0 results in location 1,0"() {
        when:
        moveShip(0, Direction.EAST)

        then:
        getShip(0).position == new Position(1, 0)
    }

    def "Ordering a ship to move south from 0,0 results in location 0, 1"() {
        when:
        moveShip(0, Direction.SOUTH)

        then:
        getShip(0).position == new Position(0, 1)
    }

    def "With two ships, ordering the first south results in it a location of 0, 1"() {
        setup:
        def ship1 = engine.createShip(1, 0, 0)
        when:
        moveShip(0, Direction.SOUTH)
        then:
        getShip(0).position == new Position(0, 1)
        ship1.position == new Position(1, 0)
    }

    def "Moving north from 0, 0 results in 0, 1, wrapping around the map"() {
        when:
        moveShip(0, Direction.NORTH)

        then:
        getShip(0).position == new Position(0, 2)
    }

    def "Moving east from 0,0 updates the gameMap at 1,0 with the ship"() {
        when:
        moveShip(0, Direction.EAST)

        def ship = getShip(0)
        then:
        game.gameMap[ship].ship == ship
    }

    def "A ship at the shipyard at updateFrame adds its halite to the player's reserves"() {
        setup:
        getShip(0).halite = 100
        moveShipToShipyard(0)
        when:
        engine.updateFrame()
        then:
        player.halite == 5100
    }

    def "A ship at the shipyard at updateFrame will have its hold emptied"() {
        setup:
        getShip(0).halite = 100
        moveShipToShipyard(0)
        when:
        engine.updateFrame()
        then:
        getShip(0).halite == 0
    }

    def "A ship moving into the shipyard empties its hold"() {
        setup:
        def ship = getShip(0)
        ship.position = player.shipyard.position.directionalOffset(Direction.EAST)
        ship.halite = 100
        when:
        moveShip(0, Direction.WEST)
        then:
        getShip(0).halite == 0
    }

    def "A ship moving into the shipyard adds its hold to the player's reserves"() {
        def ship = getShip(0)
        setup:
        ship.position = player.shipyard.position.directionalOffset(Direction.EAST)
        ship.halite = 100
        when:
        moveShip(0, Direction.WEST)
        then:
        player.halite == 5100
    }

    def "A ship moving retains its halite"() {
        setup:
        engine.createShip(1, 0, 100)
        when:
        moveShip(1, Direction.WEST)
        then:
        getShip(1).halite == 100
    }

    def "A ship with 100 halite moving from a spot with 100 halite results in the ship having 90 halite"() {
        setup:
        def ship = engine.createShip(0, 0, 100)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.SOUTH)
        then:
        ship.halite == 90
    }

    def "When a ship does not move, it is not destroyed"() {
        when:
        doNothingTurn()
        then:
        getShip(0).active
    }

    def "A ship with 0 halite can not leave a cell with halite due to insufficient resources"() {
        setup:
        def ship = engine.createShip(1, 0, 0)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.SOUTH)
        then:
        ship.position == new Position(1, 0)
    }

    def "When a ship on a cell with 0 halite issues a stay still command, it's halite hold does not change"() {
        def ship = engine.createShip(1, 0, 100)
        when:
        moveShip(1, Direction.STILL)
        then:
        ship.halite == 100
    }

    def "When a ship on a cell with 100 halite issues a stay still command, it's hold increases to 125"() {
        def ship = engine.createShip(1, 0, 100)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.STILL)
        then:
        ship.halite == 125
    }

    def "When a ship on a cell with 100 halite issues a stay still command, and it's current hold is 1000 it's hold does not increase"() {
        def ship = engine.createShip(1, 0, 1000)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.STILL)
        then:
        ship.halite == 1000
    }

    def "When a ship on a cell with 100 halite issues a stay still command, and it's current hold is 980 it's hold increases to 1000"() {
        def ship = engine.createShip(1, 0, 980)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.STILL)
        then:
        ship.halite == 1000
    }

    def "When a ship on a cell with 100 halite issues a stay still command, the map cell is decreased to 75"() {
        def ship = engine.createShip(1, 0, 100)
        getMapCell(ship).halite = 100
        when:
        moveShip(1, Direction.STILL)
        then:
        getMapCell(ship).halite == 75
    }

    def "When a ship on a cell with 100 halite issues no command, it collects 25 halite"() {
        setup:
        def ship = engine.createShip(1, 0, 100)
        getMapCell(ship).halite = 100
        when:
        doNothingTurn()
        then:
        ship.halite == 125
    }

    def "When a ship on a cell with 100 halite issues no command, and it's current hold is 1000 it's hold does not increase"() {
        def ship = engine.createShip(1, 0, 1000)
        getMapCell(ship).halite = 100
        when:
        doNothingTurn()
        then:
        ship.halite == 1000
    }

    def "When a ship on a cell with 100 halite issues no command, and it's current hold is 980 it's hold increases to 1000"() {
        def ship = engine.createShip(1, 0, 980)
        getMapCell(ship).halite = 100
        when:
        doNothingTurn()
        then:
        ship.halite == 1000
    }

    def "When a ship on a cell with 100 halite does not issue a command, the map cell is decreased to 75"() {
        def ship = engine.createShip(1, 0, 100)
        getMapCell(ship).halite = 100
        when:
        doNothingTurn()
        then:
        getMapCell(ship).halite == 75
    }

    def "When a ship moves from 0,0 to 1,0 that is already occupied, both are destroyed"() {
        setup:
        engine.createShip(1, 0, 0)
        when:
        moveShip(0, Direction.EAST)
        then:
        getShip(0).destroyed
        getShip(1).destroyed
    }

    def "When a ship moves from 0,0 to 1,0 that is already occupied by a destroyed ship, the move is successful"() {
        setup:
        def deadShip = engine.createShip(1, 0, 0)
        deadShip.destroy()
        when:
        moveShip(0, Direction.EAST)
        then:
        getShip(0).active
        getShip(0).position == new Position(1, 0)
    }

    def "When a ship is already destroyed, it doesn't update when told to move"() {
        setup:
        ship.destroy()
        when:
        engine.endTurn([Command.move(ship.id, Direction.EAST)])
        engine.updateFrame()
        then:
        ship.position == new Position(0,0)
    }
}
