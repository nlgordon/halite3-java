package com.insanedev.fakeengine

import com.insanedev.hlt.Command
import com.insanedev.hlt.Direction
import com.insanedev.hlt.Game
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Player
import com.insanedev.hlt.Ship
import spock.lang.Specification

class BaseTestFakeGameEngine extends Specification {
    FakeGameEngine engine
    Game game
    Player player

    def setup() {
        engine = new FakeGameEngine(Player.create(0, 0, 0), 2, 2)
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
}
