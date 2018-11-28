package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Direction
import com.insanedev.hlt.Position
import reactor.core.publisher.Flux
import spock.lang.Unroll

class TestPlayerStrategy extends BaseTestFakeGameEngine {
    PlayerStrategy strategy

    def setup() {
        createEngine(0, 8, 16, 32, 32)
        strategy = new PlayerStrategy(engine)
        game = strategy.init()
        player = game.me
    }

    def "PlayerStrategy map analysis creates an single area of 1x1 since only 5,5 has halite"() {
        def position = new Position(5, 5)
        game.gameMap[position].halite = 1000
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 1, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 3x1 since only 4,5 5,5 and 6,5 have halite"() {
        def position = new Position(5, 5)
        game.gameMap[new Position(4, 5)].halite = 900
        game.gameMap[new Position(5, 5)].halite = 1000
        game.gameMap[new Position(6, 5)].halite = 900
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 3, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 3x1 that wraps around from left to right"() {
        def position = new Position(0, 5)
        game.gameMap[new Position(-1, 5)].halite = 900
        game.gameMap[new Position(0, 5)].halite = 1000
        game.gameMap[new Position(1, 5)].halite = 900
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 3, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 3x1 since only 4,5 5,5 and 6,5 have halite above 50% of 5,5"() {
        def position = new Position(5, 5)
        game.gameMap[new Position(3, 5)].halite = 100
        game.gameMap[new Position(4, 5)].halite = 900
        game.gameMap[new Position(5, 5)].halite = 1000
        game.gameMap[new Position(6, 5)].halite = 900
        game.gameMap[new Position(7, 5)].halite = 100
        when:
        strategy.analyzeMap()
        then:
        assertAreaMatches(strategy.areas[0], 3, 1, position)
    }

    def "PlayerStrategy map analysis creates an single area of 1x3 since only 5,4 5,5 and 5,6 have halite"() {
        def position = new Position(5, 5)
        game.gameMap[new Position(5, 4)].halite = 900
        game.gameMap[new Position(5, 5)].halite = 1000
        game.gameMap[new Position(5, 6)].halite = 900
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 1, 3, position)
    }

    def "PlayerStrategy map analysis creates an single area of 1x3 that wraps around from top to bottom"() {
        def position = new Position(5, 0)
        game.gameMap[new Position(5, -1)].halite = 900
        game.gameMap[new Position(5, 0)].halite = 1000
        game.gameMap[new Position(5, 1)].halite = 900
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 1, 3, position)
    }

    def "PlayerStrategy map analysis creates an single area of 1x3 since only 5,4 5,5 and 5,6 have halite greater than 50% of 5,5"() {
        def position = new Position(5, 5)
        game.gameMap[new Position(5, 3)].halite = 100
        game.gameMap[new Position(5, 4)].halite = 900
        game.gameMap[new Position(5, 5)].halite = 1000
        game.gameMap[new Position(5, 6)].halite = 900
        game.gameMap[new Position(5, 7)].halite = 100
        when:
        strategy.analyzeMap()
        then:
        assertAreaMatches(strategy.areas[0], 1, 3, position)
    }

    def "Player Strategy map analysis creates two areas of 1x1 at 5,5 and 7,7 since those two cells are the only two with halite"() {
        def position1 = new Position(5, 5)
        def position2 = new Position(7, 7)
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
    }

    def "Player Strategy map analysis creates one area of 5x5 at 5,5 since it is the only cells above the map cell average halite"() {
        def position1 = new Position(5, 5)
        def position2 = new Position(20, 20)
        Flux.range(3, 5)
                .flatMap({ int x ->
            Flux.range(3, 5)
                    .map({ int y -> new Position(x, y)
            })
        }).subscribe({ game.gameMap[it].halite = 900 })
        game.gameMap[position1].halite = 1000
        game.gameMap[position2].halite = 20
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 1
        assertAreaMatches(strategy.areas[0], 5, 5, position1)
    }

    def "Player Strategy map analysis creates one area of 5x5 at 5,5 and another at 20,20 since it is just above the map cell average halite"() {
        def position1 = new Position(5, 5)
        def position2 = new Position(20, 20)
        Flux.range(3, 5)
                .flatMap({ int x ->
            Flux.range(3, 5)
                    .map({ int y -> new Position(x, y)
            })
        }).subscribe({ game.gameMap[it].halite = 700 })
        game.gameMap[position1].halite = 1000
        game.gameMap[position2].halite = 20
        when:
        strategy.analyzeMap()
        then:
        strategy.areas.size() == 2
        assertAreaMatches(strategy.areas[0], 5, 5, position1)
    }

    @Unroll
    def "Given an area at 10,10 of 1x1 and a ship exploring at #x,#y, an area influence vector direction will be generated as #dir due to the area influence"() {
        def position = new Position(x, y)

        def areaPosition = new Position(10, 10)
        strategy.areas.add(new Area(areaPosition, 1, 1, game))
        game.gameMap[areaPosition].halite = 100
        expect:
        strategy.getExplorationInflucence(position).direction == dir

        where:
        x  | y  | dir
        10 | 9  | Direction.SOUTH
        10 | 11 | Direction.NORTH
        9  | 10 | Direction.EAST
        11 | 10 | Direction.WEST
        11 | 12 | Direction.NORTH
        9  | 12 | Direction.NORTH
        11 | 8  | Direction.SOUTH
        9  | 8  | Direction.SOUTH
        8  | 11 | Direction.EAST
        8  | 9  | Direction.EAST
        12 | 11 | Direction.WEST
        12 | 9  | Direction.WEST
    }

    @Unroll
    def "Given an area at 10,10 of 1x1 and a ship exploring at #x,#y, an area influence vector will be generated of #ix,#iy due to the area influence"() {
        def position = new Position(x, y)
        def areaPosition = new Position(10, 10)
        strategy.areas.add(new Area(areaPosition, 1, 1, game))
        game.gameMap[areaPosition].halite = 100
        def influcence = strategy.getExplorationInflucence(position)
        expect:
        influcence.x == ix
        influcence.y == iy

        where:
        x  | y  | ix  | iy
        10 | 9  | 0   | 90
        10 | 11 | 0   | -90
        9  | 10 | 90  | 0
        11 | 10 | -90 | 0
    }

    @Unroll
    def "Given an area at 10,10 of 1x1 and a ship exploring at #x,#y, an area influence vector will be generated with cell influence of #i in direction #dir due to the area influence"() {
        def position = new Position(x, y)
        def areaPosition = new Position(10, 10)
        strategy.areas.add(new Area(areaPosition, 1, 1, game))
        game.gameMap[areaPosition].halite = 100
        def influcence = strategy.getExplorationInflucence(position)
        expect:
        influcence.appliedToDirection(dir) == i

        where:
        x  | y  | i   | dir
        10 | 9  | -90 | Direction.SOUTH
        10 | 9  | 90  | Direction.NORTH
        10 | 9  | 0   | Direction.EAST
        10 | 9  | 0   | Direction.WEST
        10 | 11 | -90 | Direction.NORTH
        10 | 11 | 0   | Direction.EAST
        10 | 11 | 90  | Direction.SOUTH
        10 | 11 | 0   | Direction.WEST
        9  | 10 | 90  | Direction.EAST
        9  | 10 | -90 | Direction.WEST
        9  | 10 | 0   | Direction.NORTH
        9  | 10 | 0   | Direction.SOUTH
        11 | 10 | 90  | Direction.WEST
        11 | 10 | -90 | Direction.EAST
        11 | 10 | 0   | Direction.NORTH
        11 | 10 | 0   | Direction.SOUTH
    }

    void assertAreaMatches(Area area, int width, int height, Position position) {
        assert area.width == width
        assert area.height == height
        assert area.center == position
    }
}
