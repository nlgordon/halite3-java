package com.insanedev.fakeengine

import com.insanedev.hlt.*
import spock.lang.Specification

import java.util.stream.IntStream

class BaseTestFakeGameEngine extends Specification {
    FakeGameEngine engine
    Game game
    Player player

    void initGame(int playerId, int shipyardX, int shipyardY, int mapWidth, int mapHeight) {
        engine = new FakeGameEngine(Player.create(playerId, shipyardX, shipyardY), mapWidth, mapHeight)
        game = engine.init()
        player = game.me
        engine.updateFrame()
    }

    void doNothingTurn() {
        engine.endTurn([])
        engine.updateFrame()
    }

    void moveShip(int shipId, Direction direction) {
        engine.endTurn([getShip(shipId).move(direction)])
        engine.updateFrame()
    }

    void spawnShip() {
        engine.endTurn([Command.spawnShip()])
        engine.updateFrame()
    }

    Ship getShip(int shipId) {
        player.getShip(shipId)
    }

    MapCell getMapCell(Ship ship) {
        return game.gameMap.at(ship)
    }

    void moveShipToShipyard(int i) {
        engine.updateShipPosition(i, 1, 1)
    }

    void runTurns(int count) {
        IntStream.range(0, count).forEach({
            navigateShips()
        })
    }

    void navigateShips() {
        engine.endTurn(player.navigateShips())
        engine.updateFrame()
    }

    Ship setupShipForNavigation(int shipId, int x, int y) {
        def ship = getShip(shipId)
        def destination = new Position(x, y)
        ship.destination = destination
        return ship
    }
}
