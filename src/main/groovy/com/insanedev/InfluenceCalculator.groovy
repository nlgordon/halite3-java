package com.insanedev

import com.insanedev.hlt.GameMap
import com.insanedev.hlt.Position
import groovy.transform.CompileStatic

@CompileStatic
class InfluenceCalculator {
    static List<Double> decayByDistance = []
    static {
        for (int i = 0; i < 128; i++) {
            decayByDistance[i] = Math.pow((double) Configurables.INFLUENCE_DECAY_RATE, i)
        }
    }

    static InfluenceVector calculateVectorWrapped(GameMap map, Position target, Position source, int halite) {
        final Position normalizedSource = map.normalize(source)
        final Position normalizedTarget = map.normalize(target)
        final int dx = normalizedTarget.x - normalizedSource.x
        final int dy = normalizedTarget.y - normalizedSource.y
        final int absDx = Math.abs(dx)
        final int absDy = Math.abs(dy)
        final int wrapped_dx = map.width - absDx
        final int wrapped_dy = map.height - absDy
        int actualDx = absDx < wrapped_dx ? dx : (int) Math.copySign(wrapped_dx, dx) * -1
        int actualDy = absDy < wrapped_dy ? dy : (int) Math.copySign(wrapped_dy, dy) * -1

        def magnitude = Math.abs(actualDx) + Math.abs(actualDy)
        if (magnitude == 0) {
            return InfluenceVector.ZERO
        }

        def haliteScaling
        if (FeatureFlags.getFlagStatus("NO_MAGNITUDE_REDUCTION")) {
            haliteScaling = decayByDistance[magnitude] * halite
        } else {
            haliteScaling = decayByDistance[magnitude] * halite / magnitude
        }
        int xHaliteInfluence = (int) (actualDx * haliteScaling)
        int yHaliteInfluence = (int) (actualDy * haliteScaling)
        return new InfluenceVector(xHaliteInfluence, yHaliteInfluence)
    }
}
