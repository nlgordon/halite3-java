package com.insanedev.fakeengine

import com.insanedev.hlt.Command
import com.insanedev.hlt.EntityId
import com.insanedev.hlt.Game
import com.insanedev.hlt.Player
import spock.lang.Specification

class TestFakeGameEngineConstructionCommands extends Specification {
    private FakeGameEngine engine
    private Game game

    def setup() {
        engine = new FakeGameEngine(Player.create(0, 1, 1), 2, 2)
        game = engine.init()
        engine.updateFrame()
    }

    def "Submitting a spawn ship command creates a ship on the subsequent update frame"() {
        when:
        spawnShip()

        then:
        this.game.me.ships.size() == 1
    }

    def "Spawning the first ship creates it with entity id 0"() {
        when:
        spawnShip()

        then:
        game.me.ships.containsKey(new EntityId(0))
    }

    def "Spawning the second ship creates it with entity id 1"() {
        setup:
        spawnShip()

        when:
        spawnShip()

        then:
        game.me.ships.containsKey(new EntityId(1))
    }

    def "Spawning a ship creates it at the shipyard location"() {
        when:
        spawnShip()

        then:
        game.me.ships[new EntityId(0)].position == game.me.shipyard.position
    }

    void spawnShip() {
        engine.endTurn([Command.spawnShip()])
        engine.updateFrame()
    }
}
