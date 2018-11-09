package com.insanedev.hlt

class Player {
    final PlayerId id
    final Shipyard shipyard
    int halite
    final Map<EntityId, Ship> ships = [:]
    final Map<EntityId, Dropoff> dropoffs = [:]

    private Player(final PlayerId id, final Shipyard shipyard) {
        this.id = id
        this.shipyard = shipyard
    }

    static Player create(int playerId, int shipyard_x, int shipyard_y) {
        new Player(new PlayerId(playerId), new Shipyard(new PlayerId(playerId), new Position(shipyard_x, shipyard_y)))
    }
}
