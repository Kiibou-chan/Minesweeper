package space.kiibou.net.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.net.common.SocketConnection
import java.net.Socket

private val mapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class JacksonClient(
    override val onConnect: () -> Unit,
    override val onMessageReceived: (JsonNode) -> Unit,
    override val onDisconnect: () -> Unit
) : Client<JsonNode> {
    override var socket: Socket? = null
    override var connection: SocketConnection? = null

    override fun objToString(t: JsonNode): String {
        return mapper.writeValueAsString(t)
    }

    override fun stringToObj(string: String): JsonNode {
        return mapper.readTree(string)
    }

}