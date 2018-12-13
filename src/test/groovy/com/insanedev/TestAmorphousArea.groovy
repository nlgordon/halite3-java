package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.MapCell
import com.insanedev.hlt.Position
import reactor.core.publisher.Flux

class TestAmorphousArea extends BaseTestFakeGameEngine {
    def setup() {
        initGame(0, 8, 16, 32, 32)
    }

    def "Area created with no cells has a flux of no cells"() {
        when:
        def area = new AmorphousArea([], game)

        then:
        area.cells.count().block() == 0
    }

    def "Area created with one cell at (5,5) will have only that cell in the list"() {
        def position = new Position(5,5)
        when:
        def area = new AmorphousArea([getCell(position)], game)

        then:
        getPositionsFromArea(area) == [position]
    }

    def "Area created with cell (5,5) will report the center to be (5,5)"() {
        def position = new Position(5,5)
        when:
        def area = new AmorphousArea([getCell(position)], game)

        then:
        area.center == position
    }

    def "Area created with cells [(5,5),(5,6),(5,7)] will report the center to be (5,7) since it has the highest halite"() {
        def positions = createLineOfYPositions(5, 3, 5)
        List<MapCell> cells = positionsToCells(positions)
        int startingHalite = 900
        Flux.fromIterable(cells).subscribe({it.halite = startingHalite += 5})
        when:
        def area = new AmorphousArea(cells, game)

        then:
        area.center == positions[2]
    }

    def "Area generation creates an amorphous area of 1x1 at 5,5 when that is the only cell with halite"() {
        def center = new Position(5, 5)
        game.gameMap[center].halite = 1000
        when:
        def areas = AmorphousArea.generate(center, 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        cells == [center]
    }

    def "Generator creates an amorphous area of [(5,5),(5,6)] when those are the only two cells with halite"() {
        def positions = [new Position(5, 5), new Position(5, 6)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(6,5),(5,5)] when those are the only cells with halite"() {
        def positions = [new Position(5, 5), new Position(6,5)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(4,5),(5,5)] when those are the only cells with halite"() {
        def positions = [new Position(5, 5), new Position(4,5)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(5,5),(5,4)] when those are the only cells with halite"() {
        def positions = [new Position(5, 5), new Position(5,4)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(5,5),(5,6),(5,7)] when those are the only cells with halite"() {
        def positions = createLineOfYPositions(5, 3, 5)
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(5,5),(5,6),(5,7),(5,8)] when those are the only cells with halite"() {
        def positions = createLineOfYPositions(5, 4, 5)
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(5,5),(5,6),(6,5),(6,6)] when those are the only cells with halite"() {
        def positions = [new Position(5,5), new Position(5,6), new Position(6,5), new Position(6,6)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions).subscribe({assert cells.contains(it)})
    }

    def "Generator creates an area of [(5,5),(5,6)] when [(6,5),(6,6)] are below the min halite"() {
        def positions = [new Position(5,5), new Position(5,6), new Position(6,5), new Position(6,6)]
        int startingHalite = 508
        positions.stream().forEach({ game.gameMap[it].halite = startingHalite -= 5 })
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        Flux.fromIterable(positions[2..3]).subscribe({assert !cells.contains(it)})
    }

    def "Generator does not include (6,6) when it is already part of an area"() {
        def positions = [new Position(5,5), new Position(5,6), new Position(6,5), new Position(6,6)]
        positions.stream().forEach({ game.gameMap[it].halite = 1000 })
        def firstArea = new AmorphousArea([game.gameMap[positions[3]]], game)
        when:
        def areas = AmorphousArea.generate(positions[0], 500, game)

        then:
        def cells = areas.getPositions().collectList().block()
        assert !cells.contains(positions[3])
    }

    def "Simple influence vector for an area of 0 halite 1 x distance away is (0, 0)"() {
        def area = new AmorphousArea([game.gameMap[new Position(5,5)]], game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(4, 5))
        then:
        influence == InfluenceVector.ZERO
    }

    def "Simple influence vector for an area of 100 halite 1 x distance away is (100, 0)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(4, 5))
        then:
        influence == new InfluenceVector(100, 0)
    }

    def "Simple influence vector for an area of 100 halite -1 x distance away is (-100, 0)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(6, 5))
        then:
        influence == new InfluenceVector(-100, 0)
    }

    def "Simple influence vector for an area of 100 halite 2 x distance away is (50, 0)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(3, 5))
        then:
        influence == new InfluenceVector(50, 0)
    }

    def "Simple influence vector for an area of 100 halite 1 y distance away is (0, 100)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(5, 4))
        then:
        influence == new InfluenceVector(0, 100)
    }

    def "Simple influence vector for an area of 100 halite -1 y distance away is (0, -100)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(5, 6))
        then:
        influence == new InfluenceVector(0, -100)
    }

    def "Simple influence vector for an area of 100 halite 2 y distance away is (0, 50)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(5, 3))
        then:
        influence == new InfluenceVector(0, 50)
    }

    def "Simple influence vector for an area of 100 halite 1x 1y distance away is (25, 25)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(4, 4))
        then:
        influence == new InfluenceVector(25, 25)
    }

    def "Simple influence vector for an area of 100 halite -1x 1y distance away is (25, 25)"() {
        def cells = [game.gameMap[new Position(5, 5)]]
        Flux.fromIterable(cells).subscribe({it.halite = 100})
        def area = new AmorphousArea(cells, game)
        when:
        def influence = area.calculateSimpleExteriorInfluence(new Position(6, 4))
        then:
        influence == new InfluenceVector(-25, 25)
    }

    private List<Position> createLineOfYPositions(int start, int count, int x) {
        return Flux.range(start, count).map({ new Position(x, it) }).collectList().block()
    }

    private List<MapCell> positionsToCells(List<Position> positions) {
        return Flux.fromIterable(positions).map({getCell(it)}).collectList().block()
    }

    List<Position> getPositionsFromArea(AmorphousArea area) {
        return area.cells.map({ it.position }).collectList().block()
    }

    MapCell getCell(Position cell) {
        return game.gameMap[cell]
    }
}
