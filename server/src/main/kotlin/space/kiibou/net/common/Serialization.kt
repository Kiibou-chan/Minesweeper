package space.kiibou.net.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

object Serial {
    private val modules: MutableSet<SerializersModule> = mutableSetOf()

    private var module = Json.Default.serializersModule

    var json: Json = Json {
        classDiscriminator = "#class"
        serializersModule = module
    }
        private set

    fun addModule(module: SerializersModule) {
        if (!modules.contains(module)) {
            this.module += module
            modules += module
            reinitializeJson()
        }
    }

    private fun reinitializeJson() {
        json = Json {
            classDiscriminator = "#class"
            serializersModule = module
        }
    }
}
