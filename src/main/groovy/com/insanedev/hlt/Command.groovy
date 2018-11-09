package com.insanedev.hlt

class Command {
    EntityId id
    Direction direction
    String command = null;

    static Command spawnShip() {
        return new Command("g")
    }

    static Command transformShipIntoDropoffSite(final EntityId id) {
        return new Command("c " + id)
    }

    static Command move(final EntityId id, final Direction direction) {
        return new Command(id, direction)
    }

    private Command(EntityId id, Direction direction) {
        this.id = id
        this.direction = direction
    }

    private Command(final String command) {
        this.command = command
    }

    String getCommand() {
        if (command) {
            return command
        } else {
            return "m $id $direction.charValue"
        }
    }

    @Override
    boolean equals(Object o) {
        if (this == o) return true
        if (o == null || getClass() != o.getClass()) return false

        Command command1 = (Command) o

        return command.equals(command1.command)
    }

    @Override
    int hashCode() {
        return command.hashCode()
    }
}
