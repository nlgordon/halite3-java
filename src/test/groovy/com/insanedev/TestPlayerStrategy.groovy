package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

import java.util.stream.IntStream

class TestPlayerStrategy extends BaseTestFakeGameEngine {
    PlayerStrategy strategy
    def setup() {
        createEngine(0, 8, 16, 32, 32)
        strategy = new PlayerStrategy(engine)
        game = strategy.init()
        player = game.me
    }

    def "PlayerStrategy map analysis creates an single area of 1x1 since only 5,5 has halite"() {
        def position = new Position(5,5)
        game.gameMap[position].halite = 1000
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 1, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 3x1 since only 4,5 5,5 and 6,5 have halite"() {
        def position = new Position(5,5)
        IntStream.rangeClosed(4, 6).forEach({x ->
            game.gameMap[new Position(x, 5)].halite = 1000
        })
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 3, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 1x3 since only 5,4 5,5 and 5,6 have halite"() {
        def position = new Position(5,5)
        IntStream.rangeClosed(4, 6).forEach({y ->
            game.gameMap[new Position(5, y)].halite = 1000
        })
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 1, 3, position)
    }

    def "Player Strategy map analysis creates two areas of 1x1 at 5,5 and 7,7 since those two cells are the only two with halite"() {
        def position1 = new Position(5,5)
        def position2 = new Position(7,7)
        game.gameMap[position1].halite = 1000
        game.gameMap[position2].halite = 1000
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 2
        strategy.areas.stream().forEach({
            if (it.center == position1) {
                assertAreaMatches(it, 1, 1, position1)
            } else if (it.center == position2) {
                assertAreaMatches(it, 1, 1, position2)
            } else {
                throw new IllegalArgumentException("Not a known area")
            }
        })
        assertAreaMatches(strategy.areas[0], 1, 1, position1)
    }

    void assertAreaMatches(Area area, int width, int height, Position position) {
        assert area.width == width
        assert area.height == height
        assert area.center == position
    }
}
