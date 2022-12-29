package space.kiibou.net

import mu.KotlinLogging
import java.net.InetSocketAddress
import java.net.Socket

private val logger = KotlinLogging.logger {  }

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
            logger.info {
                "Can't connect to [$serverHost:$serverPort] (timeout was $timeoutMs ms)"
            }

            return false
        }
    }
}
