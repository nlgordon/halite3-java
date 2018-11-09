package com.insanedev

import com.insanedev.hlt.Command
import com.insanedev.hlt.Direction
import com.insanedev.hlt.EntityId
import com.insanedev.hlt.GameMap
import com.insanedev.hlt.PlayerId
import com.insanedev.hlt.Position
import com.insanedev.hlt.Ship
import org.junit.Ignore
import org.junit.Test

class TestFakeGameEngine {

    @Test
    @Ignore
    void GameEngineMovesShipOneSpaceNorth() {
        GameMap map = new GameMap(3, 3)
        def playerId = new PlayerId(1)
        def entityId = new EntityId(1)
        Ship ship = new Ship(playerId, entityId, new Position(0, 0), 0)
        FakeGameEngine engine = new FakeGameEngine(map, [entityId: ship])

        Command command = ship.move(Direction.NORTH)
        engine.endTurn([command])
        assert engine.ships[0].position == new Position(1, 0)
    }
}

class FakeGameEngine {
    GameMap map
    Map<EntityId, Ship> ships

    FakeGameEngine(GameMap map, Map<EntityId, Ship> ships) {
        this.map = map
        this.ships = ships
    }

    void endTurn(ArrayList<Command> commands) {
        commands.each {
            def ship = ships[it.id]
            ship.position = ship.position.directionalOffset(it.direction)
        }
    }
}
