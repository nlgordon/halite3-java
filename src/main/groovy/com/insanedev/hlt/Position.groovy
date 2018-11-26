package com.insanedev.hlt

import groovy.transform.Canonical

@Canonical
class Position {
    final int x
    final int y

    Position(final int x, final int y) {
        this.x = x
        this.y = y
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

        return offset(dx, dy)
    }

    Position xOffset(int x) {
        return new Position(this.x + x, y)
    }

    Position yOffset(int y) {
        return new Position(x, this.y)
    }

    Position offset(int x, int y) {
        return new Position(this.x + x, this.y + y)
    }

    String toString() {
        return "Position: $x,$y"
    }
}
