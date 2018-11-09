package com.insanedev.hlt

class Constants {
    /** The maximum amount of halite a ship can carry. */
    static int MAX_HALITE
    /** The cost to build a single ship. */
    static int SHIP_COST
    /** The cost to build a dropoff. */
    static int DROPOFF_COST
    /** The maximum number of turns a game can last. */
    static int MAX_TURNS
    /** 1/EXTRACT_RATIO halite (rounded) is collected from a square per turn. */
    static int EXTRACT_RATIO
    /** 1/MOVE_COST_RATIO halite (rounded) is needed to move off a cell. */
    static int MOVE_COST_RATIO
    /** Whether inspiration is enabled. */
    static boolean INSPIRATION_ENABLED
    /** A ship is inspired if at least INSPIRATION_SHIP_COUNT opponent ships are within this Manhattan distance. */
    static int INSPIRATION_RADIUS
    /** A ship is inspired if at least this many opponent ships are within INSPIRATION_RADIUS distance. */
    static int INSPIRATION_SHIP_COUNT
    /** An inspired ship mines 1/X halite from a cell per turn instead. */
    static int INSPIRED_EXTRACT_RATIO
    /** An inspired ship that removes Y halite from a cell collects X*Y additional halite. */
    static double INSPIRED_BONUS_MULTIPLIER
    /** An inspired ship instead spends 1/X% halite to move. */
    static int INSPIRED_MOVE_COST_RATIO

    static void populateConstantsFromMap(Map<String, String> constantsMap) {
        SHIP_COST = getInt(constantsMap, "NEW_ENTITY_ENERGY_COST")
        DROPOFF_COST = getInt(constantsMap, "DROPOFF_COST")
        MAX_HALITE = getInt(constantsMap, "MAX_ENERGY")
        MAX_TURNS = getInt(constantsMap, "MAX_TURNS")
        EXTRACT_RATIO = getInt(constantsMap, "EXTRACT_RATIO")
        MOVE_COST_RATIO = getInt(constantsMap, "MOVE_COST_RATIO")
        INSPIRATION_ENABLED = getBoolean(constantsMap, "INSPIRATION_ENABLED")
        INSPIRATION_RADIUS = getInt(constantsMap, "INSPIRATION_RADIUS")
        INSPIRATION_SHIP_COUNT = getInt(constantsMap, "INSPIRATION_SHIP_COUNT")
        INSPIRED_EXTRACT_RATIO = getInt(constantsMap, "INSPIRED_EXTRACT_RATIO")
        INSPIRED_BONUS_MULTIPLIER = getDouble(constantsMap, "INSPIRED_BONUS_MULTIPLIER")
        INSPIRED_MOVE_COST_RATIO = getInt(constantsMap, "INSPIRED_MOVE_COST_RATIO")
    }

    private static int getInt(final Map<String, String> map, final String key) {
        return Integer.parseInt(getString(map, key))
    }

    private static double getDouble(final Map<String, String> map, final String key) {
        return Double.parseDouble(getString(map, key))
    }

    private static boolean getBoolean(final Map<String, String> map, final String key) {
        final String stringValue = getString(map, key)
        switch (stringValue) {
            case "true": return true
            case "false": return false
            default:
                Log.log("Error: constants: " + key + " constant has value of '" + stringValue +
                        "' from server. Do not know how to parse that as boolean.")
                throw new IllegalStateException()
        }
    }

    private static String getString(final Map<String, String> map, final String key) {
        if (!map.containsKey(key)) {
            Log.log("Error: constants: server did not send " + key + " constant.")
            throw new IllegalStateException()
        }
        return map.get(key)
    }
}
