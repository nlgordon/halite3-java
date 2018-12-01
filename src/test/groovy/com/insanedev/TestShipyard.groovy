package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position

class TestShipyard extends BaseTestFakeGameEngine {
    def setup() {
        initGameWithMultiplePlayers(2, 2, 14, 14, 16, 16)
    }

    def "When the player updates its shipyards they do not register as occupied if it is occupied by an enemy player"() {
        engine.createShipForPlayer(1, 2, 2, 0)
        when:
        player.updateDropoffs()

        def cell = game.gameMap[player.shipyard.position]
        then:
        !cell.occupied
        !cell.north.isNearOpponent(player)
        !cell.south.isNearOpponent(player)
        !cell.east.isNearOpponent(player)
        !cell.west.isNearOpponent(player)
    }

    def "Once the shipyard is no longer occupied by an opponent, and then by the player, it registers as occupied"() {
        def myShip = engine.createShip(1,1, 0)
        def opponentShip = engine.createShipForPlayer(1, 2, 2, 0)
        player.updateDropoffs()
        engine.updateShipPosition(opponentShip, new Position(4,4))
        engine.updateShipPosition(myShip, player.shipyard.position)
        when:
        player.updateDropoffs()

        def cell = game.gameMap[player.shipyard.position]
        then:
        cell.occupied
        !cell.north.isNearOpponent(player)
        !cell.south.isNearOpponent(player)
        !cell.east.isNearOpponent(player)
        !cell.west.isNearOpponent(player)

    }
}
