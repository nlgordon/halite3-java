package com.insanedev.hlt

class Game {
    int turnNumber
    PlayerId myId
    ArrayList<Player> players = new ArrayList<>()
    Player me
    GameMap gameMap

    Game(Player me, List<Player> players, GameMap map) {
        this.me = me
        this.players = players
        this.gameMap = map
        myId = me.id
        this.players.stream().forEach({it.game = this})
    }
}
