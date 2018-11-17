package com.insanedev.fakeengine

import com.insanedev.hlt.*
import spock.lang.Specification

class BaseTestFakeGameEngine extends Specification {
    FakeGameEngine engine
    Game game
    Player player

    def setup() {
        engine = new FakeGameEngine(Player.create(0, 1, 1), 3, 3)
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
}
