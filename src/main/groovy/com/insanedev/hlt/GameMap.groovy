package com.insanedev.hlt

import reactor.core.publisher.Flux

import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.IntStream
import java.util.stream.Stream

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
            for (int x = 0; x < width; x++) {
                cells[y][x] = new MapCell(new Position(x, y), 0)
            }
        }

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; x++) {
                def cell = cells[y][x]
                cell.north = cells[normalizeY(y-1)][x]
                cell.south = cells[normalizeY(y+1)][x]
                cell.east = cells[y][normalizeX(x+1)]
                cell.west = cells[y][normalizeX(x-1)]
            }
        }
    }

    void addShip(Ship ship) {
        at(ship).ship = ship
    }

    MapCell getAt(final Position position) {
        return at(position)
    }

    MapCell at(final Position position) {
        final Position normalized = normalize(position)
        return cells[normalized.y][normalized.x]
    }

    MapCell getAt(final Entity entity) {
        return at(entity)
    }

    MapCell at(final Entity entity) {
        return at(entity.position)
    }

    MapCell at(int x, int y) {
        return at(new Position(x, y))
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

    int calculateXDistance(final Position source, final Position target) {
        final Position normalizedSource = normalize(source)
        final Position normalizedTarget = normalize(target)
        final int dx = normalizedTarget.x - normalizedSource.x
        final int absDx = Math.abs(dx)
        final int wrapped_dx = width - absDx
        return absDx < wrapped_dx ? dx : (int) Math.copySign(wrapped_dx, dx) * -1
    }

    int calculateYDistance(final Position source, final Position target) {
        final Position normalizedSource = normalize(source)
        final Position normalizedTarget = normalize(target)
        final int dy = normalizedTarget.y - normalizedSource.y
        final int absDy = Math.abs(dy)
        final int wrapped_dy = height - absDy
        return absDy < wrapped_dy ? dy : (int) Math.copySign(wrapped_dy, dy) * -1
    }

    Position normalize(final Position position) {
        return new Position(normalizeX(position.x), normalizeY(position.y))
    }

    int normalizeY(int y) {
        return ((y % height) + height) % height
    }

    int normalizeX(int x) {
        return ((x % width) + width) % width
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

    void iterateOverCells(Consumer<MapCell> action) {
        IntStream.range(0, width).forEach({ x ->
            IntStream.range(0, height).forEach({ y ->
                action.accept(cells[y][x])
            })
        })
    }

    Stream<MapCell> streamCells() {
        IntStream.range(0, height).mapToObj({cells[it].collect().stream()}).flatMap(Function.identity())
    }

    Flux<MapCell> fluxOfCells() {
        Flux.range(0, height).flatMap({Flux.fromArray(cells[it])})
    }
}
