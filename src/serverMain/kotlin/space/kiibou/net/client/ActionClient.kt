package space.kiibou.net.client

import kotlinx.serialization.PolymorphicSerializer
import space.kiibou.net.common.Action
import space.kiibou.net.common.Serial
import space.kiibou.net.common.SocketConnection
import java.net.Socket

class ActionClient(
    override val onConnect: () -> Unit,
    override val onMessageReceived: (Action<*>) -> Unit,
    override val onDisconnect: () -> Unit
) : Client<Action<*>> {
    override var socket: Socket? = null
    override var connection: SocketConnection? = null

    override fun stringToObj(string: String): Action<*> {
        return Serial.json.decodeFromString(PolymorphicSerializer(Action::class), string)
    }

    override fun objToString(t: Action<*>): String {
        return Serial.json.encodeToString(PolymorphicSerializer(Action::class), t)
    }
}
