package com.insanedev.hlt

import com.insanedev.Area

class MapCell {
    final Position position
    int halite
    Ship ship
    Entity structure
    Boolean occupiedOverride
    Area area
    MapCell north
    MapCell south
    MapCell east
    MapCell west

    MapCell(final Position position, final int halite) {
        this.position = position
        this.halite = halite
    }

    boolean isEmpty() {
        return ship == null && structure == null
    }

    boolean isOccupied() {
        if (occupiedOverride != null) {
            return occupiedOverride
        }
        return ship != null
    }

    boolean isNearOpponent(Player friendly) {
        if (north.ship && north.ship.player != friendly) {
            return true
        } else if (south.ship && south.ship.player != friendly) {
            return true
        } else if (east.ship && east.ship.player != friendly) {
            return true
        } else if (west.ship && west.ship.player != friendly) {
            return true
        }

        return false
    }

    boolean hasStructure() {
        return structure != null
    }

    void markUnsafe(final Ship ship) {
        this.ship = ship
    }

    String toString() {
        return "$position $occupied"
    }
}
