package com.insanedev

import com.insanedev.hlt.Game
import com.insanedev.hlt.GameMap
import com.insanedev.hlt.Log
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import groovy.transform.EqualsAndHashCode
import reactor.core.publisher.Flux
import reactor.math.MathFlux

import java.util.stream.IntStream

abstract class Area {
    protected Map<Position, MapCell> internalCells = [:]
    protected Game game
    protected GameMap map
    protected BigDecimal cachedAverageHalite
    protected Integer cachedHalite
    private minHaliteForAreaConsideration
    boolean status = true

    Area(Game game) {
        this.game = game
        this.map = game.gameMap
        minHaliteForAreaConsideration = Configurables.MIN_HALITE_FOR_AREA_CONSIDERATION
        if (FeatureFlags.getFlagStatus(game.me, "AREA_LOW_HALITE")) {
            minHaliteForAreaConsideration = minHaliteForAreaConsideration / 2
        }
    }

    abstract Position getCenter()

    Flux<MapCell> getCells() {
        return Flux.fromIterable(internalCells.values())
    }

    Flux<Position> getPositions() {
        return Flux.fromIterable(internalCells.keySet())
    }

    int getHalite() {
        if (cachedHalite == null) {
            cachedHalite = computeHalite()
        }
        return cachedHalite
    }

    private int computeHalite() {
        return MathFlux.sumInt(getCells().map({ it.halite })).block()
    }

    BigDecimal getAverageHalite() {
        if (cachedAverageHalite == null) {
            computeAverageHalite()
        }
        return cachedAverageHalite
    }

    void computeAverageHalite() {
        cachedAverageHalite = MathFlux.averageDouble(getCells().map({ it.halite })).block()
    }

    void updateStatus() {
        computeAverageHalite()
        computeHalite()
        if (averageHalite <= this.minHaliteForAreaConsideration) {
            getCells().subscribe({ it.area = null })
            status = false
        }
    }

    InfluenceVector getVectorForPosition(Position position) {
        if (isInArea(position)) {
            // Get cell with max halite, influence that direction
            return getInnerAreaInfluence(position)
        }
        def influenceVector = InfluenceCalculator.calculateVectorWrapped(map, center, position, averageHalite as int)
        Log.log("Calculating influence for area at $center for position $position $influenceVector")
        return influenceVector
    }

    private boolean isInArea(Position position) {
        return internalCells.containsKey(position)
    }

    private InfluenceVector getInnerAreaInfluence(Position position) {
        MapCell max = MathFlux.max(Flux.fromIterable(internalCells.values()), MapCell.haliteComparator).block()
        return InfluenceCalculator.calculateVectorWrapped(map, max.position, position, max.halite)
    }

    void claimCells() {
        getCells().subscribe({it.area = this})
    }

    InfluenceVector calculateSimpleExteriorInfluence(Position position) {
        //TODO: Move this check to the ship/mission instead of the area
        if (isInArea(position)) {
            return getInnerAreaInfluence(position)
        }
        if (halite) {
            final int dy = map.calculateYDistance(center, position)
            final int dx = map.calculateXDistance(center, position)
            def totalDistance = Math.abs(dx) + Math.abs(dy)
            def xRatio = dx / totalDistance
            def yRatio = dy / totalDistance

            def yInfluence = (halite / totalDistance * yRatio).toInteger()
            def xInfluence = (halite / totalDistance * xRatio).toInteger()

            return new InfluenceVector(xInfluence, yInfluence)
        }
        return InfluenceVector.ZERO
    }
}

@EqualsAndHashCode(includes = ["center", "width", "height"])
class SquareArea extends Area {
    Position center
    int width
    int height

    SquareArea(Position center, int width, int height, Game game) {
        super(game)
        if (width % 2 != 1) {
            throw new IllegalArgumentException("Width must be odd, was $width")
        }
        if (height % 2 != 1) {
            throw new IllegalArgumentException("Height must be odd, was $height")
        }
        this.center = center
        this.width = width
        this.height = height

        collectCoveredCells()
        claimCells()
    }

    void collectCoveredCells() {
        def widthSpan = (width - 1) / 2
        int left = center.x - widthSpan
        int right = center.x + widthSpan

        def heightSpan = (height - 1) / 2
        int top = center.y - heightSpan
        int bottom = center.y + heightSpan

        IntStream.rangeClosed(left, right).forEach({ x ->
            IntStream.rangeClosed(top, bottom).forEach({ y ->
                def position = new Position(x, y)
                def cell = map[position]

                internalCells[position] = cell
            })
        })
    }

    String toString() {
        return "SquareArea: $center $width:$height $averageHalite"
    }
}

class AmorphousArea extends Area {
    Game game
    private Position cachedCenter

    static AmorphousArea generate(Position start, int minHalite, Game game) {
        def cells = []
        def cellsToSearchAround = new HashSet<MapCell>()
        cellsToSearchAround << game.gameMap[start]

        while (cellsToSearchAround) {
            cellsToSearchAround = Flux.fromIterable(cellsToSearchAround).flatMap({
                if (it.halite > minHalite && !it.area) {
                    cells << it
                    return Flux.just(it.south, it.north, it.east, it.west)
                }
                return Flux.empty()
            }).filter({!cells.contains(it)}).distinct().collectList().block()
        }
        return new AmorphousArea(cells, game)
    }

    AmorphousArea(List<MapCell> cells, Game game) {
        super(game)
        internalCells = Flux.fromIterable(cells).collectMap({it.position}).block()
        claimCells()
    }

    @Override
    Position getCenter() {
        if (cachedCenter) {
            return cachedCenter
        }
        return computeCenter()
    }

    private Position computeCenter() {
        return cachedCenter = MathFlux.max(getCells(), MapCell.haliteComparator).block().position
    }

    String toString() {
        def cells = internalCells.values().size()
        return "AmorphousArea $cells $center $averageHalite $halite"
    }

    @Override
    void updateStatus() {
        super.updateStatus()
        computeCenter()
    }
}
