package space.kiibou.net;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetUtils {

    /**
     * check if a given server is listening on a given port in the limit of timeoutMs<br>
     * from: <a href="https://gist.github.com/boly38/0a043df5c9a9427d867a4cd16162ac5c">GitHub</a>
     * </a>
     *
     * @param serverHost server hostname (or ip)
     * @param serverPort server port
     * @param timeoutMs  timeout in ms
     * @return true if a connection is possible, false otherwise
     */
    public static boolean checkServerListening(String serverHost, int serverPort, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(serverHost, serverPort), timeoutMs);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("Can't connect to [%s:%d] (timeout was %d ms), - %s",
                    serverHost, serverPort, timeoutMs, e.getMessage());
            System.out.println(errMsg);
            return false;
        }
    }

}
