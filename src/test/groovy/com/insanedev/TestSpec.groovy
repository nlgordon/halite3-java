package com.insanedev

import com.insanedev.hlt.*
import spock.lang.Specification

class TestSpec extends Specification {

    Ship ship
    Game game
    Player me
    GameMap gameMap

    def setup() {
        me = Player.create(0, 0, 0)
        gameMap = new GameMap(1, 1)
        game = new Game(me, [me], gameMap)
        ship = new Ship(game, me, new EntityId(0), new Position(0, 0), 0)
        gameMap[ship].ship = ship
        me.ships[ship.id] = ship
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
}
