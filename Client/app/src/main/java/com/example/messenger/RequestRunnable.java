package com.example.messenger;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class RequestRunnable implements Runnable {

    onConnectionLostCallback conLostCallback;
    onDoneCallback callback;
    String request;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public interface onDoneCallback {
        void done(String result);
    }

    public interface onConnectionLostCallback {
        void requestFailed();
    }

    public RequestRunnable(String request){
        this.request = request;
    }

    public RequestRunnable(String request, onDoneCallback callback, onConnectionLostCallback conLostCallback){
        this.request = request;
        this.callback = callback;
        this.conLostCallback = conLostCallback;
    }

    @Override
    public void run() {
        Socket socket = SocketHandler.getSocket();

        String result;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            bufferedWriter.write(request);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            if(callback != null) {
                int i = bufferedReader.read();

                if (i == -1) {
                    callback.done(new RequestBuilder().putRequest("CONNECTION_CLOSED").build());
                    conLostCallback.requestFailed();
                }
                result = bufferedReader.readLine();
                if(result == null) {
                    conLostCallback.requestFailed();
                }
                else {
                    callback.done(result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if(callback != null) {
                callback.done(new RequestBuilder().putRequest("CONNECTION_CLOSED").build());
                conLostCallback.requestFailed();
            }
        }
    }
}
