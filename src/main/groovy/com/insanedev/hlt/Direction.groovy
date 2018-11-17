package com.insanedev.hlt

enum Direction {
    NORTH('n'),
    EAST('e'),
    SOUTH('s'),
    WEST('w'),
    STILL('o')

    final String charValue

    final static ArrayList<Direction> ALL_CARDINALS = new ArrayList<>()
    static {
        ALL_CARDINALS.add(NORTH)
        ALL_CARDINALS.add(SOUTH)
        ALL_CARDINALS.add(EAST)
        ALL_CARDINALS.add(WEST)
    }

    Direction invertDirection() {
        switch (this) {
            case NORTH: return SOUTH
            case EAST: return WEST
            case SOUTH: return NORTH
            case WEST: return EAST
            case STILL: return STILL
            default: throw new IllegalStateException("Unknown direction " + this)
        }
    }

    List<Direction> getPerpendiculars() {
        switch (this) {
            case {it == NORTH || it == SOUTH}: return [EAST, WEST]
            case {it == EAST || it == WEST}: return [NORTH, SOUTH]
        }
    }

    Direction(final String charValue) {
        this.charValue = charValue
    }
}
