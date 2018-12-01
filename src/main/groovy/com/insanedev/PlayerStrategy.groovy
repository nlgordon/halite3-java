package com.insanedev

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import groovy.transform.Canonical
import reactor.core.publisher.Flux

import java.time.LocalTime
import java.time.temporal.ChronoUnit

interface PlayerStrategyInterface {

    InfluenceVector getExplorationInfluence(Position position)
}

class PlayerStrategy implements PlayerStrategyInterface {
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
        def start = LocalTime.now()
        game = engine.init()
        me = game.me
        me.strategy = this
        gameMap = game.gameMap
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.

        def duration = ChronoUnit.MILLIS.between(start, LocalTime.now())
        Log.log("Init duration: $duration")
        return game
    }

    void analyzeMap() {
        def start = LocalTime.now()
        MapAnalyzer analyzer = new MapAnalyzer(game)
        areas = analyzer.generateAreas()

        Flux.fromIterable(areas).subscribe({ Log.log("Area: $it.center $it.width:$it.height $it.averageHalite") })
        def duration = ChronoUnit.MILLIS.between(start, LocalTime.now())
        Log.log("Map analysis duration: $duration")
    }

    void ready() {
        engine.ready("MyJavaBot")
        Log.log("Successfully created bot! My Player ID is " + game.myId)
    }

    // TODO: Not Tested
    void assignAttackShips() {
        if (game.turnNumber > Constants.MAX_TURNS * Configurables.TURNS_UNTIL_ATTACK_SHIPYARDS) {
            Flux.fromIterable(game.players)
                    .filter({ it != me })
                    .filter({
                !Flux.fromIterable(attackShips).filter({ Ship ship -> ship.destination == it.shipyard.position }).blockFirst()
            })
                    .subscribe({
                Log.log("Need to attack $it.id")
                Ship ship = Flux.fromIterable(me.ships.values())
                        .filter({ !it.destination })
                        .sort({Ship left, Ship right -> right.halite.compareTo(left.halite)})
                        .blockFirst()
                if (ship) {
                    Log.log("Assigning $ship.id to attack player $it.id at $it.shipyard.position")
                    ship.destination = it.shipyard.position
                    attackShips.add(ship)
                }
            })

            Flux.fromIterable(attackShips)
                    .filter({ it.position == it.destination && it.status != ShipStatus.HOLDING })
                    .subscribe({ it.status = ShipStatus.HOLDING })
        }
    }

    void handleFrame() {
        def start = LocalTime.now()
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
        def duration = ChronoUnit.MILLIS.between(start, LocalTime.now())
        Log.log("Turn duration: $duration")
    }

    Command spawnShip() {
        Log.log("Spawning Ship")
        def spawnShipCommand = me.shipyard.spawn()
        return spawnShipCommand
    }

    boolean shouldSpawnShip() {
        return game.turnNumber <= Constants.MAX_TURNS * Configurables.TURNS_TO_MAKE_SHIPS && me.halite >= Constants.SHIP_COST && !gameMap[me.shipyard].occupied
    }

    @Override
    InfluenceVector getExplorationInfluence(Position position) {
        return Flux.fromIterable(areas)
                .filter({it.averageHalite > Configurables.MIN_HALITE_FOR_AREA_CONSIDERATION })
                .map({ it.getVectorForPosition(position) })
                .reduce(InfluenceVector.ZERO, { InfluenceVector accumulator, InfluenceVector addition ->
            accumulator.add(addition)
        })
                .defaultIfEmpty(InfluenceVector.ZERO)
                .block()
    }
}

@Canonical
class InfluenceVector {
    static final InfluenceVector ZERO = new InfluenceVector(0, 0)

    final int x
    final int y

    InfluenceVector(final int x, final int y) {
        this.x = x
        this.y = y
    }

    int appliedToDirection(Direction dir) {
        if (dir == Direction.NORTH) {
            return -this.y
        } else if (dir == Direction.SOUTH) {
            return this.y
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
        } else {
            if (x > 0) {
                return Direction.EAST
            } else {
                return Direction.WEST
            }
        }
    }

    InfluenceVector add(InfluenceVector other) {
        return new InfluenceVector(this.x + other.x, this.y + other.y)
    }
}
