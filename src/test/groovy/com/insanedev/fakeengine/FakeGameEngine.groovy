package com.insanedev.fakeengine

import com.insanedev.hlt.*
import com.insanedev.hlt.engine.*

class FakeGameEngine implements GameEngine {
    public static final BigDecimal HARVEST_RATIO = 0.25
    public static final BigDecimal MOVE_COST_RATIO = 0.1
    Player me
    final int mapHeight
    final int mapWidth
    private Game game
    private Collection<Command> turnCommands = []
    private maxShipId = 0
    private maxDropoffId = 0
    private turn = 0
    List<Player> players

    FakeGameEngine(Player me, int mapWidth, int mapHeight) {
        this([me], mapWidth, mapHeight)
    }

    FakeGameEngine(List<Player> players, int mapWidth, int mapHeight) {
        this.me = players[0]
        this.mapHeight = mapHeight
        this.mapWidth = mapWidth
        this.players = players
    }

    @Override
    Game init() {
        Log.safeOpen(3)
        Map<String, String> constants = [
                "NEW_ENTITY_ENERGY_COST"   : "1000",
                "DROPOFF_COST"             : "4000",
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
        game = new Game(me, players, map)
        players.stream().forEach({map[it.shipyard].structure = it.shipyard})
        return game
    }

    // TODO: destroyed ships don't have an updated position
    @Override
    void updateFrame() {
        turn++
        game.turnNumber = turn
        List<GameUpdate> updates = []
        List<ShipUpdate> shipUpdates = []
        List<MapCellUpdate> mapCellUpdates = []
        List<DropoffUpdate> dropOffUpdates = []

        PlayerUpdate playerUpdate = new PlayerUpdate(game, new PlayerId(0), me.halite, shipUpdates, dropOffUpdates)

        if (turn == 1) {
            playerUpdate.halite = 5000
        }

        shipUpdates.addAll(turnCommands.findAll { it.type == CommandType.SPAWN }.take(1).collect {
            if (playerUpdate.halite >= Constants.SHIP_COST) {
                playerUpdate.halite -= Constants.SHIP_COST
                return getNewlySpawnedShipUpdate()
            }
            return null
        }.findAll { it != null })

        Map<EntityId, MoveCommand> moveCommands = getMoveCommandMapById()
        Map<EntityId, ConstructDropoffCommand> dropOffCommands = getDropoffCommandsById()

        updates.addAll(getUpdatesFromPlayerShips(moveCommands, dropOffCommands))

        shipUpdates.addAll(getUpdatesFromGenericList(ShipUpdate, updates))
        mapCellUpdates.addAll(getUpdatesFromGenericList(MapCellUpdate, updates))
        dropOffUpdates.addAll(getUpdatesFromGenericList(DropoffUpdate, updates))

        dropOffUpdates.stream().forEach({ playerUpdate.halite -= it.cost })

        shipUpdates.stream().forEach { ShipUpdate shipUpdate ->
            if (shipUpdate.position == me.shipyard.position) {
                playerUpdate.halite += shipUpdate.halite
                shipUpdate.halite = 0
            }
        }

        Map<EntityId, Ship> destroyedShips = getDestroyedShips(shipUpdates)

        shipUpdates.removeAll(shipUpdates.findAll { destroyedShips.containsKey(it.id) })

        me.applyUpdate(playerUpdate)
        mapCellUpdates.each { it.apply() }
        this.turnCommands.clear()
    }

    Map<EntityId, ConstructDropoffCommand> getDropoffCommandsById() {
        return turnCommands
                .findAll { it.type == CommandType.CONSTRUCT }
                .collect { (ConstructDropoffCommand) it }.collectEntries { [it.id, it] }
    }

    List<GameUpdate> getUpdatesFromPlayerShips(Map<EntityId, MoveCommand> moveCommands, Map<EntityId, ConstructDropoffCommand> dropoffCommands) {
        return me.ships.values()
                .findAll { it.active }
                .collect { Ship ship ->
            def currentCellHalite = game.gameMap.at(ship).halite

            if (dropoffCommands.containsKey(ship.id)) {
                return new Tuple2(new DropoffUpdate(game, new EntityId(maxDropoffId++), ship.position, ship.halite), null)
            } else if (moveCommands.containsKey(ship.id)) {
                MoveCommand moveCommand = moveCommands[ship.id]

                return getUpdatesForMove(ship, currentCellHalite, moveCommand)
            } else {
                return getStillShipUpdates(ship, currentCellHalite)
            }
        }.collectMany { [it.first, it.second] }
                .findAll { it != null }
                .collect { (GameUpdate) it }
    }

    private <T extends GameUpdate> List<T> getUpdatesFromGenericList(Class<T> clazz, List<GameUpdate> updates) {
        return updates.findAll { clazz.isInstance(it) }.collect { clazz.cast(it) }
    }

    Map<EntityId, Ship> getDestroyedShips(List<ShipUpdate> shipUpdates) {
        Map<Position, List<ShipUpdate>> currentPositions = shipUpdates.groupBy { it.position }

        Map<EntityId, Ship> destroyedShips = currentPositions
                .findAll { it.value.size() > 1 }
                .collectMany { it.value }.collectEntries { [it.id, it] }
        return destroyedShips
    }

    Tuple2<ShipUpdate, MapCellUpdate> getUpdatesForMove(Ship ship, int currentCellHalite, MoveCommand moveCommand) {
        def updatedShipHalite = ship.halite - (int) Math.ceil(currentCellHalite * MOVE_COST_RATIO)
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
        def harvest = (int) Math.ceil(game.gameMap.at(ship).halite * HARVEST_RATIO)
        harvest = (ship.halite + harvest) > Constants.MAX_HALITE ? Constants.MAX_HALITE - ship.halite : harvest
        shipUpdate = new ShipUpdate(game, ship.id, ship.position, ship.halite + harvest)
        cellUpdate = new MapCellUpdate(game, ship.position, currentCellHalite - harvest)
        return new Tuple2<ShipUpdate, MapCellUpdate>(shipUpdate, cellUpdate)
    }

    Position normalizePosition(Ship ship, MoveCommand moveCommand) {
        return game.gameMap.normalize(ship.position.directionalOffset(moveCommand.direction))
    }

    ShipUpdate getNewlySpawnedShipUpdate() {
        return new ShipUpdate(game, new EntityId(maxShipId++), me.shipyard.position, 0)
    }

    Map<EntityId, MoveCommand> getMoveCommandMapById() {
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

    Ship createShipAtShipyard() {
        return createShip(me.shipyard.position, 0)
    }

    Ship createShip(Position position, int halite) {
        return createShip(position.x, position.y, halite)
    }

    Ship createShip(int x, int y, int halite) {
        return me.ships[new ShipUpdate(game, new EntityId(maxShipId++), new Position(x, y), halite).apply(me)]
    }

    Ship createShipForPlayer(int playerId, int x, int y, int halite) {
        return players[playerId].ships[new ShipUpdate(game, new EntityId(maxShipId++), new Position(x, y), halite).apply(players[playerId])]
    }

    void updateShipPosition(Ship ship, Position position) {
        ship.update(position, ship.halite)
    }

    void updateShipPosition(int shipId, int x, int y) {
        Ship ship = me.ships[new EntityId(shipId)]
        updateShipPosition(ship, new Position(x, y))
    }
}
