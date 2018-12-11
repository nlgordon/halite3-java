package com.insanedev.fakeengine

import com.insanedev.hlt.*
import spock.lang.Specification

import java.util.stream.IntStream

class BaseTestFakeGameEngine extends Specification {
    FakeGameEngine engine
    Game game
    Player player

    void initGame(int playerId, int shipyardX, int shipyardY, int mapWidth, int mapHeight) {
        createEngine(playerId, shipyardX, shipyardY, mapWidth, mapHeight)
        game = engine.init()
        player = game.me
        engine.updateFrame()
    }

    FakeGameEngine createEngine(int playerId, int shipyardX, int shipyardY, int mapWidth, int mapHeight) {
        engine = new FakeGameEngine(Player.create(playerId, shipyardX, shipyardY), mapWidth, mapHeight)
    }

    void initGameWithMultiplePlayers(int player1ShipyardX, int player1ShipyardY, int player2ShipyardX, int player2ShipyardY, int mapWidth, int mapHeight) {
        def player1 = Player.create(0, player1ShipyardX, player1ShipyardY)
        def player2 = Player.create(1, player2ShipyardX, player2ShipyardY)
        List<Player> players = [player1, player2]
        engine = new FakeGameEngine(players, mapWidth, mapHeight)
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
        ship.navigationDestination = destination
        return ship
    }
}
