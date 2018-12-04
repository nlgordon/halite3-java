package com.insanedev

import com.insanedev.hlt.Player

class FeatureFlags {
    static Map<Integer, List<String>> flagsToPlayers = [1:["SIMPLE_NAVIGATION"]]

    static boolean getFlagStatus(Player player, String flag) {
        flagsToPlayers[player.id.id]?.contains(flag)
    }
}
