package com.insanedev

import com.insanedev.hlt.Player

class FeatureFlags {
    static Map<Integer, List<String>> flagsToPlayers = [
            0:[
//                    "DO_NOT_OVERFILL"
//                    "AREA_LOW_HALITE"
//                    "VECTOR_IN_MAP"
//                    "LOWER_MIN_CELL_AMOUNT"
//                    "WRAPPED_AREA_INFLUENCE"
//                    "UNWRAPPED_AREA_INFLUENCE"
            ],
            1: [
//                    "SIMPLE_NAVIGATION"
//                    "UNWRAPPED_AREA_INFLUENCE"
            ]
    ]

    static boolean getFlagStatus(Player player, String flag) {
        flagsToPlayers[player.id.id]?.contains(flag)
    }
}
