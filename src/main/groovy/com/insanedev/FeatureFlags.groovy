package com.insanedev

import com.insanedev.hlt.Player

class FeatureFlags {
    static Map<Integer, List<String>> flagsToPlayers = [:]
//    0: ["AREA_LOW_HALITE"]
//    0: ["VECTOR_IN_MAP"],
//    0: ["SIMPLE_NAVIGATION"],
//    0: ["LOWER_MIN_CELL_AMOUNT"],

    static boolean getFlagStatus(Player player, String flag) {
        flagsToPlayers[player.id.id]?.contains(flag)
    }
}
