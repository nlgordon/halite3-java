package com.insanedev

import com.insanedev.fakeengine.BaseTestFakeGameEngine
import com.insanedev.hlt.Position

class TestComplexPlayerStrategy extends BaseTestFakeGameEngine {
    def setup() {
        initGameWithMultiplePlayers(8, 16, 16, 16, 32, 32)
    }

    def "Need board will have no needs if there are no areas"() {
        expect:
        !strategy.getNeeds()
    }

    def "Need board will report one needed ship if there is a single area of one cell"() {
        setupSingleAreaAt1_1()
        expect:
        strategy.getNeeds().size() == 1
    }

    def "Need board need for only area will have a location of (1,1) corresponding to the center of the area at (1,1)"() {
        setupSingleAreaAt1_1()
        expect:
        strategy.getNeeds()[0].location == new Position(1,1)
    }

    def "Need board need for only area will have the area centered at (1,1)"() {
        setupSingleAreaAt1_1()
        expect:
        strategy.getNeeds()[0].area.center == new Position(1,1)
    }

    void setupSingleAreaAt1_1() {
        game.gameMap.at(1, 1).halite = 500
        strategy.analyzeMap()
    }
}
