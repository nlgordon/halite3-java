package com.insanedev

import com.insanedev.hlt.Player

class FeatureFlags {
    static int playerId = -1

    static Map<Integer, List<String>> flagsToPlayers = [
            0:[
//                    "DO_NOT_OVERFILL"
//                    "AREA_LOW_HALITE"
//                    "VECTOR_IN_MAP"
//                    "LOWER_MIN_CELL_AMOUNT"
//                    "WRAPPED_AREA_INFLUENCE"
//                    "UNWRAPPED_AREA_INFLUENCE"
//                    "NO_ALTERNATES"
            ],
            1: [
//                    "AMORPHOUS_AREAS"
//                    "SIMPLE_NAVIGATION"
//                    "UNWRAPPED_AREA_INFLUENCE"
//                    "ONE_AREA_INFLUENCE",
//                    "NO_MAGNITUDE_REDUCTION"
//                    "SIMPLE_AREA_INFLUENCE"
            ]
    ]

    static boolean getFlagStatus(Player player, String flag) {
        flagsToPlayers[player.id.id]?.contains(flag)
    }

    static boolean getFlagStatus(String flag) {
        flagsToPlayers[playerId]?.contains(flag)
    }

    static void setPlayer(Player player) {
        playerId = player.id.id
    }

    static List<String> getFlags() {
        return flagsToPlayers[playerId]
    }
}
