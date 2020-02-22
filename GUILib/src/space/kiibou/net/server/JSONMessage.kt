package space.kiibou.net.server

import processing.data.JSONObject

data class JSONMessage(val connectionHandle: Long, val message: JSONObject)