package com.insanedev.hlt

interface GameEngine {
    Game init()

    void updateFrame()

    void ready(String name)

    void endTurn(Collection<Command> commands)
}
