package space.kiibou.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kiibou.game.TileType
import space.kiibou.net.common.MessageType

@Serializable
data class GameHandle(val gameId: Long)

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

@Serializable
data class ReadyInfo(val ready: Boolean)

@Serializable
data class YourIdInfo(val id: Long)

@Serializable
enum class RoomPhase { LOBBY, PLAYING }

@Serializable
data class MemberState(val id: Long, val ready: Boolean)

@Serializable
data class RoomSummary(val handle: GameHandle, val memberCount: Int, val settings: MapInfo)

@Serializable
data class RoomListInfo(val rooms: List<RoomSummary>)

@Serializable
data class RoomStateInfo(
    val handle: GameHandle,
    val owner: Long,
    val members: List<MemberState>,
    val settings: MapInfo,
    val phase: RoomPhase,
)

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

    @Serializable
    object JoinGame : MessageType<GameHandle>(GameHandle::class)

    // Room lifecycle (client -> server)

    @Serializable
    object CreateRoom : MessageType<Unit>(Unit::class)

    @Serializable
    object ListRooms : MessageType<Unit>(Unit::class)

    @Serializable
    object JoinRoom : MessageType<GameHandle>(GameHandle::class)

    @Serializable
    object LeaveRoom : MessageType<Unit>(Unit::class)

    @Serializable
    object SetReady : MessageType<ReadyInfo>(ReadyInfo::class)

    @Serializable
    object SetSettings : MessageType<MapInfo>(MapInfo::class)

    @Serializable
    object StartGame : MessageType<Unit>(Unit::class)

    // Room lifecycle (server -> client)

    @Serializable
    object RoomList : MessageType<RoomListInfo>(RoomListInfo::class)

    @Serializable
    object RoomState : MessageType<RoomStateInfo>(RoomStateInfo::class)

    @Serializable
    object JoinRefused : MessageType<GameHandle>(GameHandle::class)

    @Serializable
    object GameStarted : MessageType<Unit>(Unit::class)

    @Serializable
    object YourId : MessageType<YourIdInfo>(YourIdInfo::class)

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
            subclass(JoinGame::class)
            subclass(CreateRoom::class)
            subclass(ListRooms::class)
            subclass(JoinRoom::class)
            subclass(LeaveRoom::class)
            subclass(SetReady::class)
            subclass(SetSettings::class)
            subclass(StartGame::class)
            subclass(RoomList::class)
            subclass(RoomState::class)
            subclass(JoinRefused::class)
            subclass(GameStarted::class)
            subclass(YourId::class)
        }
    }
}
