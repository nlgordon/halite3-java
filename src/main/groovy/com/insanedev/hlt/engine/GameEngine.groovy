package com.insanedev.hlt.engine

import com.insanedev.hlt.Command
import com.insanedev.hlt.Game

interface GameEngine {
    Game init()

    void updateFrame()

    void ready(String name)

    void endTurn(Collection<Command> commands)
}
