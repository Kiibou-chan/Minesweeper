package space.kiibou.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kiibou.TileType
import space.kiibou.data.Vec2
import space.kiibou.net.common.Action
import space.kiibou.net.common.Serial

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

object MinesweeperAction {
    @Serializable
    @SerialName("set-time")
    data class SetTime(override val data: TimeInfo) : Action<TimeInfo>()

    @Serializable
    @SerialName("reveal-tiles")
    data class RevealTiles(override val data: TilesInfo) : Action<TilesInfo>()

    @Serializable
    @SerialName("reveal-tile")
    data class RevealTile(override val data: Vec2) : Action<Vec2>()

    @Serializable
    @SerialName("win")
    object Win : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    @SerialName("loose")
    object Loose : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    @SerialName("restart")
    object Restart : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    @SerialName("toggle-flag")
    data class ToggleFlag(override val data: Vec2) : Action<Vec2>()

    @Serializable
    @SerialName("set-flag")
    data class SetFlag(override val data: FlagInfo) : Action<FlagInfo>()

    @Serializable
    @SerialName("init-map")
    data class InitMap(override val data: MapInfo) : Action<MapInfo>()

    @Serializable
    @SerialName("set-bombs-left")
    data class SetBombsLeft(override val data: Int) : Action<Int>()

    private val serializationModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(SetTime::class)
            subclass(RevealTiles::class)
            subclass(RevealTile::class)
            subclass(Win::class)
            subclass(Loose::class)
            subclass(Restart::class)
            subclass(ToggleFlag::class)
            subclass(SetFlag::class)
            subclass(InitMap::class)
            subclass(SetBombsLeft::class)
        }
    }

    init {
        Serial.addModule(serializationModule)
    }
}
