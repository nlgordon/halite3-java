package com.insanedev

import com.insanedev.hlt.Game
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import groovy.transform.EqualsAndHashCode
import reactor.core.publisher.Flux
import reactor.math.MathFlux

import java.util.stream.IntStream

@EqualsAndHashCode(includes = ["center", "width", "height"])
class Area {
    Position center
    int width
    int height
    Game game
    private Map<Position, MapCell> internalCells = [:]
    boolean status = true
    BigDecimal cachedAverageHalite
    private minHaliteForAreaConsideration

    Area(Position center, int width, int height, Game game) {
        if (width % 2 != 1) {
            throw new IllegalArgumentException("Width must be odd, was $width")
        }
        if (height % 2 != 1) {
            throw new IllegalArgumentException("Height must be odd, was $height")
        }
        this.center = center
        this.width = width
        this.height = height
        this.game = game

        collectCoveredCells()
        minHaliteForAreaConsideration = Configurables.MIN_HALITE_FOR_AREA_CONSIDERATION
        if (FeatureFlags.getFlagStatus(game.me, "AREA_LOW_HALITE")) {
            minHaliteForAreaConsideration = minHaliteForAreaConsideration / 2
        }
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
                def cell = game.gameMap[position]

                internalCells[position] = cell
                cell.area = this
            })
        })
    }

    Flux<MapCell> getCells() {
        return Flux.fromIterable(internalCells.values())
    }

    int getHalite() {
        return MathFlux.sumInt(getCells().map({ it.halite })).block()
    }

    BigDecimal getAverageHalite() {
        if (cachedAverageHalite == null) {
            computeAverageHalite()
        }
        return cachedAverageHalite
    }

    private void computeAverageHalite() {
        cachedAverageHalite = MathFlux.averageDouble(getCells().map({ it.halite })).block()
    }

    void updateStatus() {
        computeAverageHalite()
        cachedAverageHalite = averageHalite
        if (averageHalite <= this.minHaliteForAreaConsideration) {
            getCells().subscribe({ it.area = null })
            status = false
        }
    }

    InfluenceVector getVectorForPosition(Position position) {
        if (internalCells.containsKey(position)) {
            // Get cell with max halite, influence that direction
            MapCell max = Flux.fromIterable(internalCells.values())
                    .sort({ MapCell left, MapCell right -> right.halite <=> left.halite })
                    .blockFirst()
            if (FeatureFlags.getFlagStatus(game.me, "UNWRAPPED_AREA_INFLUENCE")) {
                return InfluenceCalculator.calculateVector(max.position, position, max.halite)
            }
            return InfluenceCalculator.calculateVectorWrapped(game.gameMap, max.position, position, max.halite)
        }
        if (FeatureFlags.getFlagStatus(game.me, "UNWRAPPED_AREA_INFLUENCE")) {
            return InfluenceCalculator.calculateVector(center, position, averageHalite as int)
        }
        return InfluenceCalculator.calculateVectorWrapped(game.gameMap, center, position, averageHalite as int)
    }
}
