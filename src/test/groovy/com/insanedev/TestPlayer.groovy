package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship

class TestPlayer extends BaseTestFakeGameEngine {
    Ship ship

    def setup() {
        initGame(0, 1, 1, 8, 8)
        ship = engine.createShipAtShipyard()
    }

    def "Player will not attempt navigation on destroyed ships" () {
        ship.destination = new Position(2,2)
        ship.destroy()
        when:
        navigateShips()
        then:
        ship.position == new Position(1,1)
    }

    def "Four ships in a sqaure at 2,2 and one trying to enter, navigating to each others start point will arrive in 1 turn"() {
        def ship1Start = new Position(2, 2)
        def ship2Start = new Position(3, 2)
        def ship3Start = new Position(3, 3)
        def ship4Start = new Position(2, 3)
        def ship1 = engine.createShip(ship1Start, 0)
        def ship2 = engine.createShip(ship2Start, 0)
        def ship3 = engine.createShip(ship3Start, 0)
        def ship4 = engine.createShip(ship4Start, 0)
        engine.updateShipPosition(0, 1, 2)
        ship.destination = new Position(4, 2)

        ship1.destination = ship2Start
        ship2.destination = ship3Start
        ship3.destination = ship4Start
        ship4.destination = ship1Start
        def poolOfMoves = player.possibleMovesToMapByShip(player.collectDesiredMoves())

        when:
        def moves = player.chainRequiredMoves([], poolOfMoves, poolOfMoves[ship], ship)

        then:
        moves.size() == 0
    }
}
