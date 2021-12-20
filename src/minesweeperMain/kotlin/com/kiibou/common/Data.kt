package com.kiibou.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kiibou.data.Vec2
import space.kiibou.net.common.Action
import space.kiibou.net.common.Serial

@Serializable
data class TimeInfo(val time: Int)

@Serializable
data class TilesInfo(val tiles: List<TileInfo>)

@Serializable
data class TileInfo(val x: Int, val y: Int, val type: Int)

@Serializable
data class FlagInfo(val x: Int, val y: Int, val toggle: Boolean)

@Serializable
data class MapInfo(val width: Int, val height: Int, val bombs: Int)

object MinesweeperAction {
    @Serializable
    data class SetTime(override val data: TimeInfo) : Action<TimeInfo>()

    @Serializable
    data class RevealTiles(override val data: TilesInfo) : Action<TilesInfo>()

    @Serializable
    data class RevealTile(override val data: Vec2) : Action<Vec2>()

    @Serializable
    object Win : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    object Loose : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    object Restart : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    data class ToggleFlag(override val data: FlagInfo) : Action<FlagInfo>()

    @Serializable
    data class InitMap(override val data: MapInfo) : Action<MapInfo>()

    private val serializationModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(SetTime::class)
            subclass(RevealTiles::class)
            subclass(RevealTile::class)
            subclass(Win::class)
            subclass(Loose::class)
            subclass(Restart::class)
            subclass(ToggleFlag::class)
            subclass(InitMap::class)
        }
    }

    init {
        Serial.addModule(serializationModule)
    }
}
