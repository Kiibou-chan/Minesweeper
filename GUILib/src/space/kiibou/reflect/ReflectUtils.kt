@file:Suppress("UNCHECKED_CAST")

package space.kiibou.reflect

import space.kiibou.net.server.Service
import java.lang.reflect.Field
import java.util.*
import kotlin.streams.toList

object ReflectUtils {
    fun <T> createInstance(name: String, parameterTypes: Array<Class<*>>, vararg args: Any): T {
        val clazz = Thread.currentThread().contextClassLoader.loadClass(name) as Class<T>
        if (!hasSupertype(clazz, Service::class.java))
            error(String.format("%s is not subtype of %s", name, Service::class.java.canonicalName))
        return clazz.getDeclaredConstructor(*parameterTypes).newInstance(*args)
    }

    private fun hasSupertype(clazz: Class<*>, superClazz: Class<Service>): Boolean {
        /* search all supertypes of clazz
         * do this by searching until either Type Object is reached or Type Service is found */
        var zuper = clazz.superclass
        var searching = true
        var found = false
        while (searching && !found) {
            if (zuper == Any::class.java) searching = false
            if (zuper == superClazz) found = true else zuper = zuper.superclass
        }
        return found
    }

    fun getAnnotatedFields(obj: Any, annotation: Class<out Annotation>): Array<Field> {
        return Arrays.stream(obj.javaClass.fields)
                .filter { it.isAnnotationPresent(annotation) }
                .toList().toTypedArray()
    }
}
