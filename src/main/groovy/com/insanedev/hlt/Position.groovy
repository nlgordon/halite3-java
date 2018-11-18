package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

import java.util.stream.Stream

@EqualsAndHashCode
class Position {
    final int x
    final int y

    Position(final int x, final int y) {
        this.x = x
        this.y = y
    }

    Stream<Tuple2<Direction, Position>> possibleMoves() {
        return Direction.ALL_CARDINALS.stream()
                .map({new Tuple2<Direction, Position>(it, directionalOffset(it))})
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
