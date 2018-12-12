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
        ship.navigationDestination = new Position(2,2)
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
        ship.navigationDestination = new Position(4, 2)

        ship1.navigationDestination = ship2Start
        ship2.navigationDestination = ship3Start
        ship3.navigationDestination = ship4Start
        ship4.navigationDestination = ship1Start
        def poolOfMoves = player.possibleMovesToMapByShip(player.collectDesiredMoves())

        when:
        def moves = player.chainRequiredMoves([], poolOfMoves, poolOfMoves[ship], ship)

        then:
        moves.size() == 0
    }

    def "When the strategy says to do a rollup, and a ship is exploring, it will switch to navigating to the shipyard" () {
        engine.updateShipPosition(ship, new Position(4,4))
        def strategy = Mock(PlayerStrategy)
        player.strategy = strategy
        strategy.shouldDoRollup() >> true
        when:
        player.navigateShips()
        then:
        ship.destination == player.shipyard.position
    }
}
