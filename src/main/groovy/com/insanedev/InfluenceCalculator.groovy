package com.insanedev

import com.insanedev.hlt.GameMap
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import groovy.transform.CompileStatic

@CompileStatic
class InfluenceCalculator {
    static List<Double> decayByDistance = []
    static {
        for (int i = 0; i < 128; i++) {
            decayByDistance[i] = Math.pow((double)Configurables.INFLUENCE_DECAY_RATE, i)
        }
    }

    static InfluenceVector calculateVector(Position target, Position source, int halite) {
        int dx = target.x - source.x
        int dy = target.y - source.y

//        def magnitude = Math.sqrt(dx * dx + dy * dy)
        def magnitude = Math.abs(dx) + Math.abs(dy)
        if (magnitude == 0) {
            return InfluenceVector.ZERO
        }
        def haliteScaling = decayByDistance[magnitude] * halite / magnitude
        int xHaliteInfluence = (int)(dx * haliteScaling)
        int yHaliteInfluence = (int)(dy * haliteScaling)
        return new InfluenceVector(xHaliteInfluence, yHaliteInfluence)
    }

    static InfluenceVector calculateVectorInMap(GameMap map, Position target, Position source, int halite) {
        int dx = target.x - source.x
        int dy = target.y - source.y
        int toroidal_dx = Math.abs(dx) < map.width - Math.abs(dx) ? dx : map.width - Math.abs(dx)
        int toroidal_dy = Math.abs(dy) < map.height - Math.abs(dy) ? dy : map.height - Math.abs(dy)
        def magnitude = map.calculateDistance(source, target)
        if (magnitude == 0) {
            return InfluenceVector.ZERO
        }
        def haliteScaling = decayByDistance[magnitude] * halite / magnitude
        int xHaliteInfluence = (int)(toroidal_dx * haliteScaling)
        int yHaliteInfluence = (int)(toroidal_dy * haliteScaling)
        return new InfluenceVector(xHaliteInfluence, yHaliteInfluence)
    }

    static InfluenceVector calculateInfluenceForMap(GameMap map, Position target) {
        map.fluxOfCells()
                .map({calculateVectorInMap(map, target, it.position, it.halite)})
                .reduce(new InfluenceVector(0, 0), {InfluenceVector accumulator, InfluenceVector addition ->
            accumulator.add(addition)
        }).block()
    }

    static InfluenceVector calculateInfluenceForMapIterative(GameMap map, Position target) {
        InfluenceVector ret = InfluenceVector.ZERO
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                MapCell cell = map.at(x, y)
                ret = ret.add(calculateVectorInMap(map, target, cell.position, cell.halite))
            }
        }
        return ret
    }
}
