package com.insanedev

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import com.insanedev.hlt.engine.TextGameEngine

class MyBot {
    static void main(final String[] args) {
        GameEngine gameEngine = new TextGameEngine()
        Game game = gameEngine.init()
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        gameEngine.ready("MyJavaBot")

        Log.log("Successfully created bot! My Player ID is " + game.myId)
//        Log.enableDebugging()
        final Player me = game.me
        final GameMap gameMap = game.gameMap

        for (; ;) {
            gameEngine.updateFrame()

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

            if (game.turnNumber <= Constants.MAX_TURNS * 0.4 && me.halite >= Constants.SHIP_COST && !gameMap[me.shipyard].occupied) {
                Log.log("Spawning Ship")
                turnCommands.add(me.shipyard.spawn())
            }

            gameEngine.endTurn(turnCommands)
        }
    }
}
