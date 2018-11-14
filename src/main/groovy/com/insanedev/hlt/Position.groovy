package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Position {
    final int x
    final int y

    Position(final int x, final int y) {
        this.x = x
        this.y = y
    }

    ArrayList<Position> getSurroundingCardinals() {
        final ArrayList<Position> suroundingCardinals = new ArrayList<>()

        for (final Direction d : Direction.ALL_CARDINALS) {
            suroundingCardinals.add(directionalOffset(d))
        }

        return suroundingCardinals
    }

    Position directionalOffset(final Direction d) {
        int dx
        int dy

        switch (d) {
            case NORTH:
                dx = 0
                dy = -1
                break
            case SOUTH:
                dx = 0
                dy = 1
                break
            case EAST:
                dx = 1
                dy = 0
                break
            case WEST:
                dx = -1
                dy = 0
                break
            case STILL:
                dx = 0
                dy = 0
                break
            default:
                throw new IllegalStateException("Unknown direction " + d)
        }

        return new Position(x + dx, y + dy)
    }
}
