package com.insanedev


import com.insanedev.hlt.engine.GameEngine
import com.insanedev.hlt.engine.TextGameEngine

class MyBot {
    static void main(final String[] args) {
        System.setProperty("reactor.logging.fallback", 'JDK')
        GameEngine gameEngine = new TextGameEngine()
        ComplexPlayerStrategy playerStrategy = new ComplexPlayerStrategy(gameEngine)
        playerStrategy.init()
        playerStrategy.analyzeMap()
        playerStrategy.ready()

//        Log.enableDebugging()

        for (; ;) {
            playerStrategy.handleFrame()
        }
    }
}
