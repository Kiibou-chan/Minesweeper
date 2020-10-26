package space.kiibou.net

import java.net.InetSocketAddress
import java.net.Socket

object NetUtils {
    /**
     * check if a given server is listening on a given port in the limit of timeoutMs<br></br>
     * from: [GitHub](https://gist.github.com/boly38/0a043df5c9a9427d867a4cd16162ac5c)
     *
     *
     * @param serverHost server hostname (or ip)
     * @param serverPort server port
     * @param timeoutMs  timeout in ms
     * @return true if a connection is possible, false otherwise
     */
    fun checkServerListening(serverHost: String, serverPort: Int, timeoutMs: Int): Boolean {
        try {
            Socket().use {
                it.connect(InetSocketAddress(serverHost, serverPort), timeoutMs)
                return true
            }
        } catch (e: Exception) {
            val errMsg = "Can't connect to [$serverHost:$serverPort] (timeout was $timeoutMs ms), - ${e.message}"
            println(errMsg)
            return false
        }
    }
}