package com.insanedev.hlt

class MapCell {
    final Position position
    int halite
    Ship ship
    Entity structure
    Boolean occupiedOverride

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
