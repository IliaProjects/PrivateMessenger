package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.example.messenger.databinding.ActivityStartBinding;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class StartActivity extends BaseActivity implements LoginInterface {
    private ActivityStartBinding binding;


    private final static String SHARED_PREFERENCES_KEY_USERNAME = "USERNAME";
    private final static String PREFERENCE_FILE_KEY = "SELF_INFO";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private final static String version = "1.00";
    private final String REMOTE_HOST = "54.93.95.219";
    private final String LOCAL_HOST = "127.0.0.1";

    private Context context;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_start);
        super.onCreate(savedInstanceState);

        startService(new Intent(this, CloseSocketService.class));

        binding = ActivityStartBinding.inflate(getLayoutInflater());
        sharedPref = this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        context = this;

        username = sharedPref.getString(SHARED_PREFERENCES_KEY_USERNAME, "");


        Log.e("START", username);
        connect();
        Button btn = findViewById(R.id.reconnect_to_server);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
    }

    public void connect() {
        showProgressOverlay();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String host = SocketHandler.getHost();
                Socket socket = new Socket();
                try {
                    String request = new RequestBuilder().putRequest("CHECK_VERSION").putMessage(version).build();
                    socket.connect(new InetSocketAddress(SocketHandler.getHost(), 19000), 5000);
                    SocketHandler.setSocket(socket);
                    new Thread(new RequestRunnable(request, result -> {
                            //Пришел ответ с сервера
                            Response response = new Response(result);
                            if("VERSION_OK".equals(response.head)){
                                if (username.length() > 2) {
                                    LoginTask loginTask = new LoginTask(username , context);
                                    loginTask.execute();
                                }
                                else {
                                    hideProgressOverlay();
                                    Intent intent = new Intent(context, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    overridePendingTransition(0, 0);
                                    finish();
                                }
                            } else {
                                Intent versionErrorIntent = new Intent(context, VersionErrorActivity.class);
                                context.startActivity(versionErrorIntent);
                                finish();
                            }
                        }, () -> {
                            //ошибка соединения
                            String message = "Сервер недоступен";
                            onConnectFailed(message);
                        })).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    String message = "Сервер недоступен";
                    onConnectFailed(message);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    String message = "Сервер недоступен";
                    onConnectFailed(message);
                }
            }
        }).start();
    }

    public void onConnectFailed(String message) {
        runOnUiThread(() -> {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();
            hideProgressOverlay();
            Button btn = findViewById(R.id.reconnect_to_server);
            btn.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
        });
    }

    @Override
    public void onLoginSuccess() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        overridePendingTransition(R.anim.slide_up, R.anim.slide_up);
        finish();
    }

    @Override
    public void onLoginFail(String state) {
        String message;
        switch (state){
            case "USERNAME_EXISTS":
                editor.putString(SHARED_PREFERENCES_KEY_USERNAME, "");
                editor.apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return;

            case "VERSION_ERROR":
                Intent versionErrorIntent = new Intent(this, VersionErrorActivity.class);
                context.startActivity(versionErrorIntent);
                finish();
                return;

            case "ERROR":
                message = "Ошибка соединения";  //На сервере username = null
                break;

            case "CONNECTION_FAILED":
                message = "Сервер недоступен";  //В loginTask-е выбило exception
                break;

            default:
                message = "Ошибка"; //По идее недостижимо
                break;
        }
        onConnectFailed(message);
    }
}