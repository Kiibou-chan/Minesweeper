package space.kiibou.net.server

import com.fasterxml.jackson.databind.JsonNode

data class JsonMessage(val connectionHandle: Long, val node: JsonNode)