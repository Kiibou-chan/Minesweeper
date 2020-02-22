package space.kiibou.net.common

import java.util.*
import kotlin.streams.toList

class Callbacks<R, S> {
    private val callbacks: MutableMap<Long, (R) -> S> = Collections.synchronizedMap(HashMap())

    fun addCallback(function: (R) -> S): Long {
        Objects.requireNonNull(function)
        val handle = nextHandle()
        callbacks[handle] = function
        return handle
    }

    fun removeCallback(handle: Long): ((R) -> S)? {
        return callbacks.remove(handle)
    }

    fun call(handle: Long, arg: R): S {
        return callbacks[handle]!!(arg)
    }

    fun callAll(arg: R): List<S> {
        return callbacks.values.stream()
                .map { `fun`: (R) -> S -> `fun`(arg) }
                .toList()
    }

    companion object {
        private var handleCounter: Long = 0
        private fun nextHandle(): Long {
            return handleCounter++
        }
    }

}