package com.kiibou;

import java.util.HashMap;

public enum TileType {

    RED_BOMB("tiles/red_bomb_tile.png", -2),
    BOMB("tiles/bomb_tile.png", -1),
    EMPTY("tiles/empty_tile.png", 0),
    ONE("tiles/one_tile.png", 1),
    TWO("tiles/two_tile.png", 2),
    THREE("tiles/three_tile.png", 3),
    FOUR("tiles/four_tile.png", 4),
    FIVE("tiles/five_tile.png", 5),
    SIX("tiles/six_tile.png", 6),
    SEVEN("tiles/seven_tile.png", 7),
    EIGHT("tiles/eight_tile.png", 8);

    private final String path;
    private final int lookup;

    private static final HashMap<Integer, TileType> map = new HashMap<>(16);

    static {
        for (TileType type : TileType.values()) {
            map.put(type.lookup, type);
        }
    }

    TileType(String path, int lookup) {
        this.path = path;
        this.lookup = lookup;
    }

    public String getPath() {
        return path;
    }

    public int getLookup() {
        return lookup;
    }

    public static TileType getTypeFromValue(int value) {
        return map.get(value);
    }
}
