package com.insanedev

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import groovy.transform.Canonical
import reactor.core.publisher.Flux
import reactor.math.MathFlux

import java.time.LocalTime
import java.time.temporal.ChronoUnit

interface PlayerStrategy {

    boolean shouldDoRollup()

    InfluenceVector getExplorationInfluence(Ship ship)
}

class NullPlayerStrategy implements PlayerStrategy {

    @Override
    boolean shouldDoRollup() {
        return false
    }

    @Override
    InfluenceVector getExplorationInfluence(Ship ship) {
        return InfluenceVector.ZERO
    }
}

class ComplexPlayerStrategy implements PlayerStrategy {
    GameEngine engine
    Game game
    Player me
    GameMap gameMap
    List<Area> areas = []
    List<Ship> attackShips = []

    ComplexPlayerStrategy(GameEngine engine) {
        this.engine = engine
    }

    Game init() {
        def start = LocalTime.now()
        game = engine.init()
        me = game.me
        FeatureFlags.setPlayer(me)
        Log.log("Running with flags: ${FeatureFlags.getFlags()}")
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
        areas = analyzer.generateAmorphousAreas()

        Flux.fromIterable(areas).subscribe({ Log.log("Area: $it") })
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
                    .filter({ it.id != me.id })
                    .filter({
                !Flux.fromIterable(attackShips).filter({ Ship ship -> ship.mission.destination == it.shipyard.position }).blockFirst()
            })
                    .subscribe({
                Log.log("Need to attack $it.id")
                Ship ship = Flux.fromIterable(me.ships.values())
                        .filter({ !it.destination })
                        .filter({ it.halite < 100 })
                        .sort({Ship left, Ship right -> right.halite.compareTo(left.halite)})
                        .blockFirst()
                if (ship) {
                    Log.log("Assigning $ship.id to attack player $it.id at $it.shipyard.position")
                    ship.holdAtPosition(it.shipyard.position)
                    attackShips.add(ship)
                }
            })
        }
    }

    void updateAreas() {
        Flux.fromIterable(areas).subscribe({it.updateStatus()})
    }

    void handleFrame() {
        def start = LocalTime.now()
        engine.updateFrame()
        me.updateDropoffs()
        updateAreas()
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
    boolean shouldDoRollup() {
        int remainingTurns = Constants.MAX_TURNS - game.turnNumber
        def shipyardPosition = me.shipyard.position
        int maxDistanceFromShipyard = MathFlux.max(Flux.fromStream(me.activeShips)
                .map({ it.calculateDistance(shipyardPosition)}))
                .defaultIfEmpty(0)
                .block()
        return maxDistanceFromShipyard + 2 >= remainingTurns
    }

    @Override
    InfluenceVector getExplorationInfluence(Ship ship) {
        Position position = ship.position
        if (FeatureFlags.getFlagStatus("ONE_AREA_INFLUENCE")) {
            return Flux.fromIterable(areas)
                    .filter({it.status })
                    .sort({Area left, Area right ->
                gameMap.calculateDistance(left.center, position) <=> gameMap.calculateDistance(right.center, position)
            })
                    .map({ it.getVectorForPosition(position) })
                    .defaultIfEmpty(InfluenceVector.ZERO)
                    .blockFirst()
        }
        if (FeatureFlags.getFlagStatus("SIMPLE_AREA_INFLUENCE")) {
            return MathFlux.max(Flux.fromIterable(areas)
                    .filter({it.status}), {Area left, Area right ->
                left.calculateSimpleExteriorInfluence(position).compareTo(right.calculateSimpleExteriorInfluence(position))
            })
                    .map({it.calculateSimpleExteriorInfluence(position)})
                    .defaultIfEmpty(InfluenceVector.ZERO)
                    .block()
        }
        return Flux.fromIterable(areas)
                .filter({it.status })
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

    int compareTo(InfluenceVector other) {
        return (x + y) <=> (other.x + other.y)
    }
}
