package com.insanedev


import com.insanedev.hlt.engine.GameEngine
import com.insanedev.hlt.engine.TextGameEngine

class MyBot {
    static void main(final String[] args) {
        GameEngine gameEngine = new TextGameEngine()
        PlayerStrategy playerStrategy = new PlayerStrategy(gameEngine)
        playerStrategy.init()
        playerStrategy.ready()

//        Log.enableDebugging()

        for (; ;) {
            playerStrategy.handleFrame()
        }
    }
}
