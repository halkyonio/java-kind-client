package dev.snowdrop.kind;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class PortUtils {

    private static String LOCALHOST = "127.0.0.1";

    public static int getFreePortOnHost() throws IOException {
        InetAddress addr = InetAddress.getByName(LOCALHOST);
        ServerSocket socket = new ServerSocket(0, 0, addr);
        return socket.getLocalPort();
    }
}
