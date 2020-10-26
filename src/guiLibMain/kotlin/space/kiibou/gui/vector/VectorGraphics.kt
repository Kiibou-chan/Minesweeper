package space.kiibou.gui.vector

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

typealias GraphicsElementFactory = (GApplet) -> GraphicsElement

/* TODO: 03/11/2019 \
 * Primitives:
 * arc (x, y, w, h, angle: start [radians], angle: stop [radians], mode { PIE, OPEN, CHORD })
 * circle (x, y, extent)
 * ellipse (x, y, w, h)
 * point (x, y)
 * quad (x1, y1, x2, y2, x3, y3, x4, y4)
 * square (x, y, extent)
 * triangle (x1, y1, x2, y2, x3, y3)
 *
 * Curves:
 * bezier (x1, y1, x2, y2, x3, y3, x4, y4)
 * curve (x1, y1, x2, y2, x3, y3, x4, y4)
 *  curveTightness (tightness: [-5..5])
 *
 * Modes:
 * ellipseMode (mode { RADIUS, CENTER, CORNER, CORNERS })
 * rectMode (mode { RADIUS, CENTER, CORNER, CORNERS })
 *
 * Vertex:
 * contour -> beginContour, endContour
 * bezierVertex (cx2, cy2, cx3, cy3, x4, y4)
 * curveVertex (x, y)
 * quadraticVertex (cx, cy, x, y)
 *
 * Transforms:
 * translate
 * rotate
 * scale
 * apply matrix?
 *
 * Add Events and Animations?
 */

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