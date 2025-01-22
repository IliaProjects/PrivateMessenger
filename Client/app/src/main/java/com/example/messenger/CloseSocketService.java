package com.example.messenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.net.Socket;

public class CloseSocketService extends Service {

    private Socket socket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.e("Service", "destroy");
        String request = new RequestBuilder().putRequest("LOGOUT").build();
        new Thread(new RequestRunnable(request)).start();
        super.onDestroy();

    }

}
