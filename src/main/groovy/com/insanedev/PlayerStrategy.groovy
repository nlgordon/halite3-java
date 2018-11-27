package com.insanedev

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine

class PlayerStrategy {
    GameEngine engine
    Game game
    Player me
    GameMap gameMap
    List<Area> areas = []

    PlayerStrategy(GameEngine engine) {
        this.engine = engine
    }

    Game init() {
        game = engine.init()
        me = game.me
        gameMap = game.gameMap
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.

        return game
    }

    void analyzeMap() {
        MapAnalyzer analyzer = new MapAnalyzer(game)
        areas = analyzer.generateAreas()
    }

    void ready() {
        engine.ready("MyJavaBot")
        Log.log("Successfully created bot! My Player ID is " + game.myId)
    }

    void handleFrame() {
        engine.updateFrame()
        me.updateDropoffs()

        /* Phases of Turn Loop
         *
         * Calculate Player Strategy
         *  Build ships
         *  Create target zone
         *  Create dropoff
         *  Bring all ships home when game.turnNumber + ships + movements == Constans.MAX_TURNS
         *  Attack other player
         * Calculate Player Commands
         * Calculate Ship Wants
         * Calculate Ship Moves
         */

        List<Command> turnCommands = new ArrayList<>()
        turnCommands.addAll(me.navigateShips())

        if (shouldSpawnShip()) {
            Command spawnShipCommand = spawnShip()
            turnCommands.add(spawnShipCommand)
        }

        engine.endTurn(turnCommands)
    }

    Command spawnShip() {
        Log.log("Spawning Ship")
        def spawnShipCommand = me.shipyard.spawn()
        return spawnShipCommand
    }

    boolean shouldSpawnShip() {
        return game.turnNumber <= Constants.MAX_TURNS * 0.4 && me.halite >= Constants.SHIP_COST && !gameMap[me.shipyard].occupied
    }
}
