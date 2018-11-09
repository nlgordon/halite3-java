package com.insanedev.hlt

class TextGameEngine implements GameEngine {
    Game game

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
                final int halite = rowInput.getInt()
                map.cells[y][x] = new MapCell(new Position(x, y), halite)
            }
        }

        return map
    }

    void initConstants() {
        Constants.populateConstantsFromMap(parseConstantsString(Input.readLine()))
    }

    Map<String, String> parseConstantsString(String stringFromEngine) {
        final String[] rawTokens = stringFromEngine.split("[{}, :\"]+");
        final ArrayList<String> tokens = new ArrayList<>();
        for (int i = 0; i < rawTokens.length; ++i) {
            if (!rawTokens[i].isEmpty()) {
                tokens.add(rawTokens[i]);
            }
        }

        if ((tokens.size() % 2) != 0) {
            Log.log("Error: constants: expected even total number of key and value tokens from server.");
            throw new IllegalStateException();
        }

        final Map<String, String> constantsMap = new HashMap<>();

        for (int i = 0; i < tokens.size(); i += 2) {
            constantsMap.put(tokens.get(i), tokens.get(i+1));
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
        update count
        x, y, halite
     */
    @Override
    void updateFrame() {
        game.turnNumber = Input.readInput().getInt()
        Log.log("=============== TURN " + game.turnNumber + " ================")

        for (int i = 0; i < game.players.size(); ++i) {
            final Input input = Input.readInput()

            final PlayerId currentPlayerId = new PlayerId(input.getInt())
            final int numShips = input.getInt()
            final int numDropoffs = input.getInt()
            final int halite = input.getInt()

            updatePlayer(game.players.get(currentPlayerId.id), numShips, numDropoffs, halite)
        }

        updateGameMap()
    }

    private void updatePlayer(Player player, final int numShips, final int numDropoffs, final int halite) {
        Log.debug("Updating player ${player.id} with $numShips ships, $numDropoffs dropoffs, and $halite halite")
        player.halite = halite

        List<EntityId> updatedShips = []
        for (int i = 0; i < numShips; ++i) {
            updatedShips.add(updateShip(player))
        }

        Set<EntityId> destroyedShips = new ArrayList<EntityId>(player.ships.keySet())
        destroyedShips.removeAll(updatedShips)

        if (destroyedShips) {
            Log.debug("Ships were destroyed: $destroyedShips")
        }

        destroyedShips.forEach{
            player.ships[it].destroy()
        }

        Log.debug("Player Ships: ${player.ships.keySet()}")

        player.dropoffs.clear()
        for (int i = 0; i < numDropoffs; ++i) {
            updateDropOff(player)
        }
    }

    private void updateDropOff(Player player) {
        final Input input = Input.readInput()

        final EntityId dropoffId = new EntityId(input.getInt())
        final int x = input.getInt()
        final int y = input.getInt()

        Dropoff dropoff = player.dropoffs[dropoffId]
        if (!dropoff) {
            dropoff = new Dropoff(player.id, dropoffId, new Position(x, y))
            player.dropoffs[dropoffId] = dropoff
            game.gameMap.at(dropoff).structure = dropoff
        }
    }

    private EntityId updateShip(Player player) {
        final Input input = Input.readInput()

        final EntityId shipId = new EntityId(input.getInt())
        final int x = input.getInt()
        final int y = input.getInt()
        final int halite = input.getInt()

        Ship ship = player.ships[shipId]
        if (ship) {
            game.gameMap.at(ship).ship = null
            ship.position = new Position(x, y)
            ship.halite = halite
        } else {
            ship = player.ships[shipId] = new Ship(player, shipId, new Position(x, y), halite)
        }
        game.gameMap.at(ship).markUnsafe(ship)

        Log.debug("Updating player ${player.id} ship ${ship.id} at ${ship.position.x} ${ship.position.y} with halite ${ship.halite}")

        return shipId
    }

    private void updateGameMap() {
        final int updateCount = Input.readInput().getInt()

        for (int i = 0; i < updateCount; ++i) {
            final Input input = Input.readInput()
            final int x = input.getInt()
            final int y = input.getInt()

            game.gameMap.cells[y][x].halite = input.getInt()
        }
    }

    @Override
    void ready(String name) {
        System.out.println(name)
    }

    @Override
    void endTurn(Collection<Command> commands) {
        for (Command command : commands) {
            System.out.print(command.command)
            System.out.print(' ')
        }
        System.out.println()
    }
}
