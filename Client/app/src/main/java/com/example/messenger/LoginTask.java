package com.example.messenger;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LoginTask extends AsyncTask<Void, Void, String> {
    private final Context context;
    private final String username;


    public LoginTask(String userName, Context context) {
        this.username = userName;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            String host = SocketHandler.getHost();
            Socket socket = SocketHandler.getSocket();

            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String request = new RequestBuilder().putRequest("LOGIN").addParam("name=" + username).build();
            bufferedWriter.write(request);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            int state = bufferedReader.read();
            Response response;
            if (state != -1){
                response = new Response(bufferedReader.readLine());
            } else {
                return "CONNECTION_FAILED";
            }
            return response.head;
        } catch (IOException e) {
            e.printStackTrace();
            return "CONNECTION_FAILED";
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "CONNECTION_FAILED";
        }
    }

    @Override
    protected void onPostExecute(String state) {
        super.onPostExecute(state);
        String s = state;
        if (s.equals("LOGIN_OK")) {
            User.setMyUsername(this.username);
            ((LoginInterface)context).onLoginSuccess();
        }
        else {
            ((LoginInterface)context).onLoginFail(s);
        }

    }
}
