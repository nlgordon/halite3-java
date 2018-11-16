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
            case Direction.NORTH:
                dx = 0
                dy = -1
                break
            case Direction.SOUTH:
                dx = 0
                dy = 1
                break
            case Direction.EAST:
                dx = 1
                dy = 0
                break
            case Direction.WEST:
                dx = -1
                dy = 0
                break
            case Direction.STILL:
                dx = 0
                dy = 0
                break
            default:
                throw new IllegalStateException("Unknown direction " + d)
        }

        return new Position(x + dx, y + dy)
    }
}
