package com.insanedev.hlt.engine

import com.insanedev.hlt.*

class TextGameEngine implements GameEngine {
    Game game

    @Override
    void ready(String name) {
        System.out.println(name)
    }

    @Override
    void endTurn(Collection<Command> commands) {
        commands.stream().filter({it != null}).forEach({
            System.out.print(it.command)
            System.out.print(' ')
        })
        System.out.println()
    }

    /* Order of data to read off of input:
    Read Constants
    Number of players, player id for me
    Each player:
        id, shipyard x & y
    Game Map: width, height
    Each Row:
        Each column: halite
     */

    @Override
    Game init() {
        ArrayList<Player> players = []
        Player me
        GameMap gameMap

        initConstants()

        final Input input = Input.readInput()
        final int numPlayers = input.getInt()
        PlayerId myId = new PlayerId(input.getInt())

        Log.open(myId.id)

        Log.log("My ID is $myId")

        for (int i = 0; i < numPlayers; ++i) {
            players.add(initPlayer())
        }
        me = players.get(myId.id)

        gameMap = initGameMap()

        return game = new Game(me, players, gameMap)
    }

    Player initPlayer() {
        final Input input = Input.readInput()

        int playerId = input.getInt()
        int shipYardX = input.getInt()
        int shipYardY = input.getInt()

        Log.log("Initialized player $playerId with shipyard at $shipYardX $shipYardY")

        return Player.create(playerId, shipYardX, shipYardY)
    }

    GameMap initGameMap() {
        final Input mapInput = Input.readInput()
        final int width = mapInput.getInt()
        final int height = mapInput.getInt()

        final GameMap map = new GameMap(width, height)

        for (int y = 0; y < height; ++y) {
            final Input rowInput = Input.readInput()

            for (int x = 0; x < width; ++x) {
                map.cells[y][x].halite = rowInput.getInt()
            }
        }

        return map
    }

    void initConstants() {
        Constants.populateConstantsFromMap(parseConstantsString(Input.readLine()))
    }

    Map<String, String> parseConstantsString(String stringFromEngine) {
        final String[] rawTokens = stringFromEngine.split("[{}, :\"]+")
        final ArrayList<String> tokens = new ArrayList<>()
        for (int i = 0; i < rawTokens.length; ++i) {
            if (!rawTokens[i].isEmpty()) {
                tokens.add(rawTokens[i])
            }
        }

        if ((tokens.size() % 2) != 0) {
            Log.log("Error: constants: expected even total number of key and value tokens from server.")
            throw new IllegalStateException()
        }

        final Map<String, String> constantsMap = new HashMap<>()

        for (int i = 0; i < tokens.size(); i += 2) {
            constantsMap.put(tokens.get(i), tokens.get(i + 1))
        }

        return constantsMap
    }

    /* Order of data to read of off input:
    Turn Number
    Each Player:
        id, number of ships, number of dropoffs, halite
        Each ship for player:
            id, x, y, halite
        Each dropoff for player:
            id, x, y
     GameMap
        applyUpdate count
        x, y, halite
     */

    @Override
    void updateFrame() {
        game.turnNumber = Input.readInput().getInt()
        Log.log("=============== TURN " + game.turnNumber + " ================")

        (0..<game.players.size()).collect({
            Log.debug("Reading player: $it")
            readPlayerUpdate()
        }).each({
            game.players[it.id.id].applyUpdate(it)
        })

        updateGameMap()
    }

    private PlayerUpdate readPlayerUpdate() {
        final Input input = Input.readInput()

        final PlayerId currentPlayerId = new PlayerId(input.getInt())
        final int numShips = input.getInt()
        final int numDropoffs = input.getInt()
        final int halite = input.getInt()

        return new PlayerUpdate(game,
                currentPlayerId,
                halite,
                readShipUpdates(numShips),
                readDropoffUpdates(numDropoffs))
    }

    private List<ShipUpdate> readShipUpdates(int numberOfShips) {
        return (0..<numberOfShips).collect({
            readShipUpdate()
        })
    }

    private ShipUpdate readShipUpdate() {
        final Input input = Input.readInput()

        final EntityId shipId = new EntityId(input.getInt())
        final int x = input.getInt()
        final int y = input.getInt()
        final int halite = input.getInt()

        return new ShipUpdate(game, shipId, new Position(x, y), halite)
    }

    private List<DropoffUpdate> readDropoffUpdates(int numberOfDropoffs) {
        (0..<numberOfDropoffs).collect({
            readDropoffUpdate()
        })
    }

    private DropoffUpdate readDropoffUpdate() {
        final Input input = Input.readInput()

        final EntityId dropoffId = new EntityId(input.getInt())
        final int x = input.getInt()
        final int y = input.getInt()

        return new DropoffUpdate(game, dropoffId, new Position(x, y))
    }

    private void updateGameMap() {
        readGameMapUpdates().each({
            it.apply()
        })
    }

    private List<MapCellUpdate> readGameMapUpdates() {
        final int updateCount = Input.readInput().getInt()

        (0..<updateCount).collect({
            return readMapCellUpdate()
        })
    }

    private MapCellUpdate readMapCellUpdate() {
        final Input input = Input.readInput()
        final int x = input.getInt()
        final int y = input.getInt()
        final int halite = input.getInt()

        return new MapCellUpdate(game, new Position(x, y), halite)
    }
}
