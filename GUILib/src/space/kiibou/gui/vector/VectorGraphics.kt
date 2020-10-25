package space.kiibou.gui.vector

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

typealias GraphicsElementFactory = (GApplet) -> GraphicsElement

class VectorGraphics(app: GApplet, x: Int, y: Int, width: Int, height: Int, scale: Int, dataPath: String) : GraphicsElement(app, x, y, width, height) {
    private val data = mapper.readTree(VectorGraphics::class.java.classLoader.getResourceAsStream(dataPath))

    init {
        data.at("/elements").forEach { node ->
            val type = node.at("/type").asText()
            val data = node.at("/data")
            val c: Class<out GraphicsElementFactory>

            c = try {
                @Suppress("UNCHECKED_CAST")
                VectorGraphics::class.java.classLoader.loadClass(type) as Class<out GraphicsElementFactory>
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                return@forEach
            }

            try {
                val elem: GraphicsElement = mapper.treeToValue(data, c)!!.invoke(app)
                addChild(elem)
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
            }
        }
    }

}

private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)