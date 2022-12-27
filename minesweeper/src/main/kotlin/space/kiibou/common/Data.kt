package space.kiibou.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kiibou.game.TileType
import space.kiibou.net.common.MessageType

@Serializable
data class TimeInfo(val time: Int)

@Serializable
data class TilesInfo(val tiles: List<TileInfo>)

@Serializable
data class TileInfo(val x: Int, val y: Int, val type: TileType)

@Serializable
data class FlagInfo(val x: Int, val y: Int, val status: Boolean)

@Serializable
data class MapInfo(val width: Int, val height: Int, val bombs: Int)

@Serializable
data class TilePosition(val x: Int, val y: Int)

@Serializable
data class BombsLeftInfo(val bombs: Int)

object MinesweeperMessageType {
    @Serializable
    object SetTime : MessageType<TimeInfo>(TimeInfo::class)

    @Serializable
    object RevealTile : MessageType<TilePosition>(TilePosition::class)

    @Serializable
    object RevealTiles : MessageType<TilesInfo>(TilesInfo::class)

    @Serializable
    object Win : MessageType<Unit>(Unit::class)

    @Serializable
    object Loose : MessageType<Unit>(Unit::class)

    @Serializable
    object Restart : MessageType<Unit>(Unit::class)

    @Serializable
    object ToggleFlag : MessageType<TilePosition>(TilePosition::class)

    @Serializable
    object SetFlag : MessageType<FlagInfo>(FlagInfo::class)

    @Serializable
    object SetBombsLeft : MessageType<BombsLeftInfo>(BombsLeftInfo::class)

    @Serializable
    object InitMap : MessageType<MapInfo>(MapInfo::class)

    val serializersModule = SerializersModule {
        polymorphic(MessageType::class) {
            subclass(SetTime::class)
            subclass(RevealTile::class)
            subclass(RevealTiles::class)
            subclass(Win::class)
            subclass(Loose::class)
            subclass(Restart::class)
            subclass(ToggleFlag::class)
            subclass(SetFlag::class)
            subclass(SetBombsLeft::class)
            subclass(InitMap::class)
        }
    }
}
