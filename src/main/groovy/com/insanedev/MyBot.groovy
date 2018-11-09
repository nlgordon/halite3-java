package com.insanedev

import com.insanedev.hlt.*

class MyBot {
    static void main(final String[] args) {
        final long rngSeed
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1])
        } else {
            rngSeed = System.nanoTime()
        }
        final Random rng = new Random(rngSeed)

        GameEngine gameEngine = new TextGameEngine()
        Game game = gameEngine.init()
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        gameEngine.ready("MyJavaBot")

        Log.log("Successfully created bot! My Player ID is " + game.myId)
//        Log.enableDebugging()

        for (; ;) {
            gameEngine.updateFrame()
            final Player me = game.me
            final GameMap gameMap = game.gameMap

            final ArrayList<Command> commandQueue = new ArrayList<>()

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

            Log.log("Deciding actions for ships: ${me.ships.keySet()}")

            for (final Ship ship : me.ships.values()) {
                if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10 || ship.isFull()) {
                    final Direction randomDirection = Direction.ALL_CARDINALS.get(rng.nextInt(4))
                    Log.log("Moving ${ship.id} $randomDirection")
                    commandQueue.add(ship.move(randomDirection))
                } else {
                    Log.log("Not moving ${ship.id}")
                    commandQueue.add(ship.stayStill())
                }
            }

            if (game.turnNumber <= Constants.MAX_TURNS / 2 && me.halite >= Constants.SHIP_COST && !gameMap.at(me.shipyard).isOccupied()) {
                Log.log("Spawning Ship")
                commandQueue.add(me.shipyard.spawn())
            }

            gameEngine.endTurn(commandQueue)
        }
    }
}
