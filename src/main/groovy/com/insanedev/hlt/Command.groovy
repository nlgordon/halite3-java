package com.insanedev.hlt

import groovy.transform.EqualsAndHashCode

enum CommandType {
    MOVE("m"), SPAWN("g"), CONSTRUCT("c")

    final String charValue

    CommandType(String charValue) {
        this.charValue = charValue
    }
}

@EqualsAndHashCode
class Command {
    CommandType type

    static Command spawnShip() {
        return new Command(CommandType.SPAWN)
    }

    static ConstructDropoffCommand transformShipIntoDropoffSite(final EntityId id) {
        return new ConstructDropoffCommand(id)
    }

    static MoveCommand move(final EntityId id, final Direction direction) {
        return new MoveCommand(id, direction)
    }

    Command(CommandType type) {
        this.type = type
    }

    String getCommand() {
        return type.charValue
    }
}

class MoveCommand extends Command {
    EntityId id
    Direction direction

    MoveCommand(final EntityId id, final Direction direction) {
        super(CommandType.MOVE)
        this.id = id
        this.direction = direction
    }

    String getCommand() {
        return "$type.charValue $id $direction.charValue"
    }
}

class ConstructDropoffCommand extends Command {
    EntityId id

    ConstructDropoffCommand(final EntityId id) {
        super(CommandType.CONSTRUCT)
        this.id = id
    }

    String getCommand() {
        return "$type.charValue $id"
    }
}
