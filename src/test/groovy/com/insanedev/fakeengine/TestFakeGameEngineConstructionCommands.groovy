package com.insanedev.fakeengine


import com.insanedev.hlt.Position

class TestFakeGameEngineConstructionCommands extends BaseTestFakeGameEngine {
    def "Submitting a spawn ship command creates a ship on the subsequent update frame"() {
        when:
        spawnShip()

        then:
        player.ships.size() == 1
    }

    def "Spawning the first ship creates it with entity id 0"() {
        when:
        spawnShip()

        then:
        getShip(0) != null
    }

    def "Spawning the second ship creates it with entity id 1"() {
        setup:
        engine.insertShip(1, 0, 0)

        when:
        spawnShip()

        then:
        getShip(1) != null
    }

    def "Spawning a ship creates it at the shipyard location"() {
        when:
        spawnShip()

        then:
        getShip(0).position == player.shipyard.position
    }

    def "Spawning the first ship leaves the player with 4000 halite"() {
        when:
        spawnShip()

        then:
        player.halite == 4000
    }

    def "Spawning two ships leaves the player with 3000 halite"() {
        when:
        spawnShip()
        spawnShip()

        then:
        player.halite == 3000
    }

    def "Spawning a ship starts it with 0 halite"() {
        when:
        spawnShip()

        then:
        getShip(0).halite == 0
    }

    def "The fake engine can insert an arbitrary ship"() {
        expect:
        engine.insertShip() == 0
    }

    def "Inserting two ships results in ship ids of 0, 1"() {
        expect:
        engine.insertShip() == 0
        engine.insertShip() == 1
    }

    def "Inserting a ship creates the ship entity in the me player"() {
        when:
        engine.insertShip()

        then:
        getShip(0) != null
    }

    def "Inserting a ship creates the ship at 0,0"() {
        when:
        engine.insertShip()

        then:
        getShip(0).position == new Position(0, 0)
    }

    def "Inserting a ship at 1,1 creates the ship at 1,1"() {
        when:
        engine.insertShip(1, 1)

        then:
        getShip(0).position == new Position(1, 1)
    }

    def "Inserting a ship at 1,1 with 100 halite results in a ship with 100 halite"() {
        when:
        engine.insertShip(1, 1, 100)

        then:
        getShip(0).halite == 100
    }

    def "Spawning a ship when the player has zero halite does not create the ship"() {
        setup:
        player.halite = 0
        when:
        spawnShip()
        then:
        player.ships.size() == 0
    }

    def "Ordering a ship to convert to a dropoff at 1,1 will create a dropoff at next frame update"() {
        setup:
        def ship = engine.createShip(1, 1, 4000)
        when:
        engine.endTurn([ship.makeDropoff()])
        engine.updateFrame()
        then:
        player.dropoffs.size() == 1
    }

    // Deduct from player halite
    // Ensure ship is destroyed
}
