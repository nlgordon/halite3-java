package com.insanedev

import com.insanedev.hlt.Game
import com.insanedev.hlt.GameMap
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.math.MathFlux

class MapAnalyzer {
    GameMap map
    Game game
    private int searchDistance

    MapAnalyzer(Game game) {
        this.game = game
        this.map = game.gameMap
        searchDistance = map.width / Configurables.AREA_SEARCH_DISTANCE_RATIO
    }

    List<Area> generateAreas() {
        int averageHaliteInCells = getAverageHalitePerCell()
        return Flux.fromStream(map.streamCells())
                .filter({ it.halite > averageHaliteInCells * Configurables.AREA_AVERAGE_MULTIPLIER })
                .sort({ MapCell left, MapCell right -> right.halite <=> left.halite })
                .filter({ it.area == null })
                .flatMap({
            def position = it.position
            def minHaliteForArea = getMinHaliteForArea(position)
            def width = getAreaWidth(position, minHaliteForArea)
            def height = getAreaHeight(position, minHaliteForArea)
            Mono.zip(width, height).map({new Area(position, (int)it.t1, (int)it.t2, game)})
        }).collectList().block()
    }

    double getAverageHalitePerCell() {
        return MathFlux.averageDouble(Flux.fromStream(map.streamCells()).map({ it.halite })).block()
    }

    Mono<Long> getAreaWidth(Position position, int minHalite) {
        return getAreaDimension(position.x, searchDistance, { int x -> new Position(x, position.y) }, minHalite)
    }

    Mono<Long> getAreaHeight(Position position, int minHalite) {
        return getAreaDimension(position.y, searchDistance, { int y -> new Position(position.x, y) }, minHalite)
    }

    int getMinHaliteForArea(Position position) {
        return map[position].halite * Configurables.AREA_MINIMUM_SCALE
    }

    Mono<Long> getAreaDimension(int start, int maxDistance, Closure<Position> positionMapping, int minHalite) {
        Flux<Position> rightPositions = Flux.range(start + 1, maxDistance).map(positionMapping)
        Flux<Position> leftPositions = Flux.range(start - maxDistance, maxDistance).sort(Collections.reverseOrder()).map(positionMapping)

        return getDimension(rightPositions, leftPositions, minHalite)
    }

    Mono<Long> getDimension(Flux<Position> firstPositions, Flux<Position> oppositePositions, int minimumHaliteForArea) {
        Mono<Long> first = countCellsForArea(firstPositions, minimumHaliteForArea)
        Mono<Long> opposite = countCellsForArea(oppositePositions, minimumHaliteForArea)

        return Mono.zip(first, opposite).map({1 + Math.min(it.t1, it.t2) * 2})
    }

    Mono<Long> countCellsForArea(Flux<Position> positions, int minimumHaliteForArea) {
        return positions
                .takeWhile({ map[it].halite > minimumHaliteForArea })
                .count()
    }
}
