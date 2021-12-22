package com.kiibou

enum class TileType(val path: String, val lookup: Int) {
    NO_BOMB("tiles/no_bomb_tile.png", -3),
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

    companion object {
        private val map = HashMap<Int, TileType>(16)

        fun getTypeFromValue(value: Int): TileType {
            return map[value]!!
        }

        init {
            values().forEach { map[it.lookup] = it }
        }
    }
}
