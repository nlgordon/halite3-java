package com.insanedev.hlt

class GameMap {
    int width
    int height
    MapCell[][] cells

    GameMap(final int width, final int height) {
        this.width = width
        this.height = height

        cells = new MapCell[height][]
        for (int y = 0; y < height; ++y) {
            cells[y] = new MapCell[width]
        }
    }

    MapCell at(final Position position) {
        final Position normalized = normalize(position)
        return cells[normalized.y][normalized.x]
    }

    MapCell at(final Entity entity) {
        return at(entity.position)
    }

    int calculateDistance(final Position source, final Position target) {
        final Position normalizedSource = normalize(source)
        final Position normalizedTarget = normalize(target)

        final int dx = Math.abs(normalizedSource.x - normalizedTarget.x)
        final int dy = Math.abs(normalizedSource.y - normalizedTarget.y)

        final int toroidal_dx = Math.min(dx, width - dx)
        final int toroidal_dy = Math.min(dy, height - dy)

        return toroidal_dx + toroidal_dy
    }

    Position normalize(final Position position) {
        final int x = ((position.x % width) + width) % width
        final int y = ((position.y % height) + height) % height
        return new Position(x, y)
    }

    ArrayList<Direction> getUnsafeMoves(final Position source, final Position destination) {
        final ArrayList<Direction> possibleMoves = new ArrayList<>()

        final Position normalizedSource = normalize(source)
        final Position normalizedDestination = normalize(destination)

        final int dx = Math.abs(normalizedSource.x - normalizedDestination.x)
        final int dy = Math.abs(normalizedSource.y - normalizedDestination.y)
        final int wrapped_dx = width - dx
        final int wrapped_dy = height - dy

        if (normalizedSource.x < normalizedDestination.x) {
            possibleMoves.add(dx > wrapped_dx ? Direction.WEST : Direction.EAST)
        } else if (normalizedSource.x > normalizedDestination.x) {
            possibleMoves.add(dx < wrapped_dx ? Direction.WEST : Direction.EAST)
        }

        if (normalizedSource.y < normalizedDestination.y) {
            possibleMoves.add(dy > wrapped_dy ? Direction.NORTH : Direction.SOUTH)
        } else if (normalizedSource.y > normalizedDestination.y) {
            possibleMoves.add(dy < wrapped_dy ? Direction.NORTH : Direction.SOUTH)
        }

        return possibleMoves
    }

    Direction naiveNavigate(final Ship ship, final Position destination) {
        // getUnsafeMoves normalizes for us
        for (final Direction direction : getUnsafeMoves(ship.position, destination)) {
            final Position targetPos = ship.position.directionalOffset(direction)
            if (!at(targetPos).isOccupied()) {
                at(targetPos).markUnsafe(ship)
                return direction
            }
        }

        return Direction.STILL
    }
}
