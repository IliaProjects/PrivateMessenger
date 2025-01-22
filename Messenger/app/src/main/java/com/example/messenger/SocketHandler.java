package com.example.messenger;

import java.net.Socket;

public class SocketHandler {
    private static final String HOST = "54.93.95.219";
    private static final String LOCAL_HOST = "10.0.2.2";

    private static Socket socket;

    public static synchronized String getHost(){
        return LOCAL_HOST;
    }

    public static synchronized Socket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(Socket socket){
        SocketHandler.socket = socket;
    }
}
