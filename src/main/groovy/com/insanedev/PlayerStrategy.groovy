package com.insanedev

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import reactor.core.publisher.Flux

class PlayerStrategy {
    GameEngine engine
    Game game
    Player me
    GameMap gameMap
    List<Area> areas = []
    List<Ship> attackShips = []

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

    // TODO: Not Tested
    void assignAttackShips() {
        Flux.fromIterable(game.players)
                .filter({it != me})
                .filter({ !Flux.fromIterable(attackShips).filter({Ship ship -> ship.destination == it.shipyard.position}).blockFirst() })
                .subscribe({
            Log.log("Need to attack $it.id")
            Ship ship = Flux.fromIterable(me.ships.values()).filter({!it.destination}).blockFirst()
            if (ship) {
                Log.log("Assigning $ship.id to attack player $it.id at $it.shipyard.position")
                ship.destination = it.shipyard.position
                attackShips.add(ship)
            }
        })

        Flux.fromIterable(attackShips)
                .filter({it.position == it.destination && it.status != ShipStatus.HOLDING})
                .subscribe({it.status = ShipStatus.HOLDING})
    }

    void handleFrame() {
        engine.updateFrame()
        me.updateDropoffs()
        assignAttackShips()

        /* Phases of Turn Loop
         *
         * Calculate Player Strategy
         *  Build ships
         *  Create target zone
         *  Create dropoff
         *  Bring all ships home when game.turnNumber + ships + movements == Constants.MAX_TURNS
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

    InfluenceVector getExplorationInflucence(Position position) {
        def area = areas[0]
        int halite = area.halite
        int dx = area.center.x - position.x
        int dy = area.center.y - position.y

        int xHaliteInfluence = dx * halite * 0.9
        int yHaliteInfluence = dy * halite * 0.9
        return new InfluenceVector(xHaliteInfluence, yHaliteInfluence)
    }
}

class InfluenceVector {
    int x = 0
    int y = 0

    InfluenceVector(int x, int y) {
        this.x = x
        this.y = y
    }

    int appliedToDirection(Direction dir) {
        if (dir == Direction.NORTH) {
            return this.y
        } else if (dir == Direction.SOUTH) {
            return -this.y
        } else if (dir == Direction.EAST) {
            return this.x
        } else if (dir == Direction.WEST) {
            return -this.x
        }
        return 0
    }

    Direction getDirection() {
        int absX = Math.abs(x)
        int absY = Math.abs(y)

        if (absY > absX) {
            if (y > 0) {
                return Direction.SOUTH
            } else {
                return Direction.NORTH
            }
        } else if (absX > absY) {
            if (x > 0) {
                return Direction.EAST
            } else {
                return Direction.WEST
            }
        }
    }
}
