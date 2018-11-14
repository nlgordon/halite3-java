package com.insanedev.fakeengine

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import com.insanedev.hlt.engine.PlayerUpdate
import com.insanedev.hlt.engine.ShipUpdate

class FakeGameEngine implements GameEngine {
    Player me
    int mapHeight = 1
    int mapWidth = 1
    private Game game
    private Collection<Command> turnCommands = []
    private maxShipId = 0

    FakeGameEngine(Player me) {
        this.me = me
    }

    FakeGameEngine(Player me, int mapWidth, int mapHeight) {
        this(me)
        this.mapHeight = mapHeight
        this.mapWidth = mapWidth
    }

    @Override
    Game init() {
        Map<String, String> constants = [
                "NEW_ENTITY_ENERGY_COST"   : "1000",
                "DROPOFF_COST"             : "0",
                "MAX_ENERGY"               : "1000",
                "MAX_TURNS"                : "0",
                "EXTRACT_RATIO"            : "0",
                "MOVE_COST_RATIO"          : "0",
                "INSPIRATION_ENABLED"      : "false",
                "INSPIRATION_RADIUS"       : "0",
                "INSPIRATION_SHIP_COUNT"   : "0",
                "INSPIRED_EXTRACT_RATIO"   : "0",
                "INSPIRED_BONUS_MULTIPLIER": "0",
                "INSPIRED_MOVE_COST_RATIO" : "0",]
        Constants.populateConstantsFromMap(constants)
        GameMap map = new GameMap(mapWidth, mapHeight)
        game = new Game(me, [me], map)
        return game
    }

    @Override
    void updateFrame() {
        List<ShipUpdate> shipUpdates = []
        if (turnCommands.any { it.type == CommandType.SPAWN }) {
            shipUpdates << new ShipUpdate(game, new EntityId(maxShipId), me.shipyard.position, -1)
            maxShipId++
        }

        PlayerUpdate playerUpdate = new PlayerUpdate(game, new PlayerId(0), 5000, shipUpdates, [])
        me.applyUpdate(playerUpdate)
    }

    @Override
    void ready(String name) {

    }

    @Override
    void endTurn(Collection<Command> commands) {
        this.turnCommands = commands
    }
}
