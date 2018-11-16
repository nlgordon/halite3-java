package com.insanedev.fakeengine

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.GameEngine
import com.insanedev.hlt.engine.GameUpdate
import com.insanedev.hlt.engine.MapCellUpdate
import com.insanedev.hlt.engine.PlayerUpdate
import com.insanedev.hlt.engine.ShipUpdate

class FakeGameEngine implements GameEngine {
    Player me
    int mapHeight = 1
    int mapWidth = 1
    private Game game
    private Collection<Command> turnCommands = []
    private maxShipId = 0
    private turn = 0

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
        Log.safeOpen(0)
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
        turn++
        List<GameUpdate> updates = []
        List<ShipUpdate> shipUpdates = []
        List<MapCellUpdate> mapCellUpdates = []

        int updatedHalite = me.halite

        if (turn == 1) {
            updatedHalite = 5000
        }
        if (turnCommands.any { it.type == CommandType.SPAWN } && me.halite >= Constants.SHIP_COST) {
            shipUpdates << getNewlySpawnedShipUpdate()
            maxShipId++
            updatedHalite -= Constants.SHIP_COST
        }

        Map<EntityId, MoveCommand> moveCommands = getMoveCommandMapByShipId()

        updates += getUpdatesFromPlayerShips(moveCommands)

        shipUpdates += getShipUpdatesFromGenericList(updates)
        mapCellUpdates += getMapCellUpdatesFromGenericList(updates)

        shipUpdates.each { ShipUpdate shipUpdate ->
            if (shipUpdate.position == me.shipyard.position) {
                updatedHalite += shipUpdate.halite
                shipUpdate.halite = 0
            }
        }

        Map<EntityId, Ship> destroyedShips = getDestroyedShips(shipUpdates)

        shipUpdates = shipUpdates.findAll {!destroyedShips.containsKey(it.id)}

        PlayerUpdate playerUpdate = new PlayerUpdate(game, new PlayerId(0), updatedHalite, shipUpdates, [])
        me.applyUpdate(playerUpdate)
        mapCellUpdates.each {it.apply()}
        this.turnCommands.clear()
    }

    List<GameUpdate> getUpdatesFromPlayerShips(Map<EntityId, MoveCommand> moveCommands) {
        return me.ships.values()
                .findAll { it.active }
                .collect { Ship ship ->
            def currentCellHalite = game.gameMap.at(ship).halite

            if (moveCommands.containsKey(ship.id)) {
                MoveCommand moveCommand = moveCommands[ship.id]

                return getUpdatesForMove(ship, currentCellHalite, moveCommand)
            } else {
                return getStillShipUpdates(ship, currentCellHalite)
            }
        }.collectMany { [it.first, it.second] }
                .findAll { it != null }
    }

    List<MapCellUpdate> getMapCellUpdatesFromGenericList(List<GameUpdate> updates) {
        return updates.findAll { it instanceof MapCellUpdate }.collect { (MapCellUpdate) it }
    }

    List<ShipUpdate> getShipUpdatesFromGenericList(List<GameUpdate> updates) {
        return updates.findAll { it instanceof ShipUpdate }.collect { (ShipUpdate) it }
    }

    Map<EntityId, Ship> getDestroyedShips(List<ShipUpdate> shipUpdates) {
        Map<Position, List<ShipUpdate>> currentPositions = shipUpdates.groupBy { it.position }

        Map<EntityId, Ship> destroyedShips = currentPositions
                .findAll { it.value.size() > 1 }
                .collectMany { it.value }.collectEntries { [it.id, it] }
        return destroyedShips
    }

    Tuple2<ShipUpdate, MapCellUpdate> getUpdatesForMove(Ship ship, int currentCellHalite, MoveCommand moveCommand) {
        def updatedShipHalite = ship.halite - (int) Math.ceil(currentCellHalite * 0.1)
        if (updatedShipHalite < 0 || moveCommand.direction == Direction.STILL) {
            return getStillShipUpdates(ship, currentCellHalite)
        } else {
            def shipUpdate = new ShipUpdate(game, moveCommand.id, normalizePosition(ship, moveCommand), updatedShipHalite)
            return new Tuple2<ShipUpdate, MapCellUpdate>(shipUpdate, null)
        }
    }

    Tuple2<ShipUpdate, MapCellUpdate> getStillShipUpdates(Ship ship, int currentCellHalite) {
        ShipUpdate shipUpdate
        MapCellUpdate cellUpdate
        def harvest = (int) Math.ceil(game.gameMap.at(ship).halite * 0.25)
        harvest = (ship.halite + harvest) > 1000 ? 1000 - ship.halite : harvest
        shipUpdate = new ShipUpdate(game, ship.id, ship.position, ship.halite + harvest)
        cellUpdate = new MapCellUpdate(game, ship.position, currentCellHalite - harvest)
        return new Tuple2<ShipUpdate, MapCellUpdate>(shipUpdate, cellUpdate)
    }

    Position normalizePosition(Ship ship, MoveCommand moveCommand) {
        return game.gameMap.normalize(ship.position.directionalOffset(moveCommand.direction))
    }

    ShipUpdate getNewlySpawnedShipUpdate() {
        return new ShipUpdate(game, new EntityId(maxShipId), me.shipyard.position, 0)
    }

    Map<EntityId, MoveCommand> getMoveCommandMapByShipId() {
        return turnCommands
                .findAll { it.type == CommandType.MOVE }
                .collect { (MoveCommand) it }
                .collectEntries({ [it.id, it] })
    }


    @Override
    void ready(String name) {

    }

    @Override
    void endTurn(Collection<Command> commands) {
        this.turnCommands = commands
    }

    Ship createShip(int x, int y, int halite) {
        return me.ships[new ShipUpdate(game, new EntityId(maxShipId++), new Position(x, y), halite).apply(me)]
    }

    int insertShip() {
        return insertShip(0, 0)
    }

    int insertShip(int x, int y) {
        return insertShip(x, y, 0)
    }

    int insertShip(int x, int y, int halite) {
        return new ShipUpdate(game, new EntityId(maxShipId++), new Position(x, y), halite).apply(me).id
    }
}
