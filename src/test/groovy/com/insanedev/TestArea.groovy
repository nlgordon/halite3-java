package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

import java.util.stream.IntStream

class TestArea extends BaseTestFakeGameEngine {
    def setup() {
        initGame(0, 8, 16, 32, 32)
    }

    def "An area has a center position"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 5, 7, game)
        then:
        area.center == new Position(5, 5)
    }

    def "An area has a width"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 5, 7, game)
        then:
        area.width == 5
    }

    def "An area has a height"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 5, 7, game)
        then:
        area.height == 7
    }

    def "An area with a width of 2 throws an IllegalArgumentException"() {
        when:
        new Area(new Position(5,5), 2, 1, game)
        then:
        thrown(IllegalArgumentException)
    }

    def "An area with a height of 2 throws an IllegalArgumentException"() {
        when:
        new Area(new Position(5,5), 1, 2, game)
        then:
        thrown(IllegalArgumentException)
    }

    def "An area of 1x1 at location 5,5 will contain the map cell 5,5"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 1, 1, game)
        then:
        area.cells.filter({
            it.position == center
        }).count() == 1
    }

    def "An area of 3x1 at location 5,5 will contain the rectangle of cells from 4,5 to 6,5"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 3, 1, game)
        then:
        area.cells.filter({
            it.position.x >= 4 && it.position.x <= 6 &&
                    it.position.y == 5
        }).count() == 3
    }

    def "An area of 3x5 at location 5,5 will contain the rectangle of cells from 4,3 to 6,7"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 3, 5, game)
        then:
        area.cells.filter({
            it.position.x >= 4 && it.position.x <= 6 &&
                    it.position.y >= 3 && it.position.y <= 7
        }).count() == 15
    }

    def "An area of 1x1 with 0 halite reports 0 halite for total"() {
        expect:
        new Area(new Position(5,5), 1, 1, game).halite == 0
    }

    def "An area of 1x1 with 100 halite in that cell reports 100 halite for total"() {
        def position = new Position(5, 5)
        game.gameMap[position].halite = 100
        expect:
        new Area(position, 1, 1, game).halite == 100
    }

    def "An area of 3x3 with halite at cells of 1,2,3,4,5,6,7,8,9 reports 45"() {
        int count = 1
        IntStream.rangeClosed(4, 6).forEach({ x ->
            IntStream.rangeClosed(4, 6).forEach({ y ->
                def position = new Position(x, y)
                game.gameMap[position].halite = count++
            })
        })
        def center = new Position(5, 5)
        expect:
        new Area(center, 3, 3, game).halite == 45
    }

    def "An area of 3x5 at 5,5 will flag the map cells as being attached to an area"() {
        def center = new Position(5, 5)
        when:
        def area = new Area(center, 3, 5, game)
        then:
        IntStream.rangeClosed(4, 6).forEach({ x ->
            IntStream.rangeClosed(3, 7).forEach({ y ->
                def position = new Position(x, y)
                assert game.gameMap[position].area == area
            })
        })
    }

    def "When a ship is within an area, it receives influence based on max halite for cells in the area"() {
        def center = new Position(5, 5)
        def area = new Area(center, 5, 5, game)
        game.gameMap.at(7,5).halite = 1000

        expect:
        area.getVectorForPosition(center).x > 0
    }
}
