package space.kiibou.net.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

object Serial {
    private val modules: MutableList<SerializersModule> = mutableListOf()
    private var initialized = false

    fun addModule(module: SerializersModule) {
        if (initialized) {
            throw IllegalStateException(
                "Can not add Modules to JSON Serializer after using it.\n" +
                        "Consider adding it at the start of the program."
            )
        }

        modules += module
    }

    val json: Json by lazy {
        initialized = true

        Json {
            serializersModule = modules.fold(Json.Default.serializersModule, SerializersModule::plus)
        }
    }
}
