package com.insanedev.hlt;

import java.util.Collection;

public interface GameEngine {
    Game init();

    void updateFrame();

    void ready(String name);

    void endTurn(Collection<Command> commands);
}
