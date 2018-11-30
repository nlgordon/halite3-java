package com.insanedev

import com.insanedev.hlt.Game
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import groovy.transform.EqualsAndHashCode
import reactor.core.publisher.Flux

import java.util.stream.IntStream
import java.util.stream.Stream

@EqualsAndHashCode(includes=["center", "width", "height"])
class Area {
    Position center
    int width
    int height
    Game game
    private Map<Position, MapCell> internalCells = [:]

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

    Stream<MapCell> getCells() {
        return internalCells.values().stream()
    }

    int getHalite() {
        return getCells().mapToInt({it.halite}).sum()
    }

    BigDecimal getAverageHalite() {
        return getCells().mapToInt({it.halite}).average().orElse(0)
    }

    InfluenceVector getVectorForPosition(Position position) {
        if (internalCells.containsKey(position)) {
            // Get cell with max halite, influence that direction
            MapCell max = Flux.fromIterable(internalCells.values())
                    .sort({MapCell left, MapCell right -> right.halite <=> left.halite})
                    .blockFirst()
            return calculateVector(max.position, position, max.halite)
        }
        return calculateVector(center, position, averageHalite as int)
    }

    InfluenceVector calculateVector(Position target, Position source, int halite) {
        int dx = target.x - source.x
        int dy = target.y - source.y

        def magnitude = Math.sqrt(dx * dx + dy * dy)
        def haliteScaling = Math.pow(Configurables.INFLUENCE_DECAY_RATE, magnitude) * halite / magnitude
        int xHaliteInfluence = dx * haliteScaling
        int yHaliteInfluence = dy * haliteScaling
        return new InfluenceVector(xHaliteInfluence, yHaliteInfluence)
    }
}
