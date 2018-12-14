package com.insanedev.hlt

import com.insanedev.Dispatcher
import com.insanedev.NullPlayerStrategy
import com.insanedev.PlayerStrategy
import com.insanedev.hlt.engine.PlayerUpdate
import groovy.transform.EqualsAndHashCode

import java.util.stream.Stream

@EqualsAndHashCode(includes = "id")
class Player {
    final PlayerId id
    Shipyard shipyard
    int halite
    final Map<EntityId, Ship> ships = [:]
    final Map<EntityId, Dropoff> dropoffs = [:]
    Game game
    PlayerStrategy strategy = new NullPlayerStrategy()
    Dispatcher dispatcher = new Dispatcher()

    private Player(final PlayerId id) {
        this.id = id
    }

    Ship getShip(int id) {
        return ships[new EntityId(id)]
    }

    static Player create(int playerId, int shipyard_x, int shipyard_y) {
        Player player = new Player(new PlayerId(playerId))
        def shipyard = new Shipyard(player, new Position(shipyard_x, shipyard_y))
        player.shipyard = shipyard
        return player
    }

    void applyUpdate(PlayerUpdate update) {
        Log.debug("Updating player $id with ${update.numberOfShips} ships, ${update.numberOfDropoffs} dropoffs, and ${update.halite} halite")
        halite = update.halite

        List<EntityId> updatedShips = update.shipUpdates.collect({
            it.apply(this)
        })

        markShipsDestroyed(updatedShips)

        Log.debug("Player Ships: ${ships.keySet()}")

        update.dropoffUpdates.each({
            it.buildDropoff(this)
        })
    }

    void markShipsDestroyed(List<EntityId> updatedShips) {
        Set<EntityId> destroyedShips = ships.values()
                .findAll { it.active }
                .collect({ it.id })
        destroyedShips.removeAll(updatedShips)

        if (destroyedShips) {
            Log.debug("Ships were destroyed: $destroyedShips")
        }

        destroyedShips.forEach {
            ships[it].destroy()
        }
    }

    Stream<Ship> getActiveShips() {
        return ships.values().stream()
                .filter({ it.active })
    }

    List<MoveCommand> navigateShips() {
        //logActiveShips()
        handleRollup()
        return dispatcher.executeDesiredMoves(getActiveShips())
    }

    void handleRollup() {
        if (strategy.shouldDoRollup()) {
            getActiveShips().forEach({ it.doDropoff() })
            game.gameMap[shipyard].occupiedOverride = false
        }
    }

    void logActiveShips() {
        Log.log("Active ships:")
        getActiveShips().forEach({
            def cell = it.game.gameMap[it]
            Log.log("$it $cell")
        })
    }

    void setGame(Game game) {
        this.game = game
        this.shipyard.game = game
    }

    void updateDropoffs() {
        Stream.concat(Stream.of(shipyard), dropoffs.values().stream()).forEach({
            def mapCell = game.gameMap[it.position]
            if (mapCell.ship && mapCell.ship.player != this) {
                mapCell.occupiedOverride = false
            } else {
                mapCell.occupiedOverride = null
            }
        })
    }
}
