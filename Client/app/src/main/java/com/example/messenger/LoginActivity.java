package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.messenger.databinding.ActivityLoginBinding;

public class LoginActivity extends BaseActivity implements LoginInterface {

    private AppBarConfiguration appBarConfiguration;
    private ActivityLoginBinding binding;
    private Context context;

    private final static String SHARED_PREFERENCES_USER_NAME= "USERNAME";
    private static String PREFERENCE_FILE_KEY = "SELF_INFO";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private String username;

    boolean toastBlocked = false;
    public void delayToast(int sec) {
        (new Thread(()->{
            try {
                toastBlocked = true;
                Thread.sleep(sec*1000);
                toastBlocked = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        context = this;
        sharedPref = this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        //Focus on input & show keyboard
        EditText useridInput = binding.useridInput;
        useridInput.requestFocus();
        InputMethodManager imm = (InputMethodManager)  getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(useridInput, InputMethodManager.SHOW_IMPLICIT);

        useridInput.requestFocus();
        setContentView(binding.getRoot());
        /*setSupportActionBar(binding.toolbar);*/

        AppCompatButton btn = binding.buttonConnectToServer;

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btn.setEnabled(false);
                showProgressOverlay();
                username = useridInput.getText().toString();


                if(SocketHandler.getSocket().isClosed()) {
                    Intent intent = new Intent(context, StartActivity.class);
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    finish();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (username.length() > 2) {
                                LoginTask task = new LoginTask(username, context);
                                task.execute();
                            }
                            else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if(!toastBlocked) {
                                            delayToast(2);
                                            Toast.makeText(context, "Никнейм содержит менее 3-х символов", Toast.LENGTH_SHORT).show();
                                        }
                                        btn.setEnabled(true);
                                        hideProgressOverlay();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn.setEnabled(true);
                                    hideProgressOverlay();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onLoginSuccess() {
        editor.putString(SHARED_PREFERENCES_USER_NAME, username);
        editor.apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        finish();
    }

    @Override
    public void onLoginFail(String state) {
        String message;
        switch (state){
            case "USERNAME_EXISTS":
                message = "Пользователь с таким именем сейчас онлайн";
                break;
            case "CONNECTION_FAILED":
                message = "Сервер недоступен";  //В loginTask-е выбило exception
                break;
            case "ERROR":
                message = "Ошибка соединения";  //На сервере username = null
                break;
            default:
                message = "Ошибка"; //По идее недостижимо
                break;
        }
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
        hideProgressOverlay();
        AppCompatButton btn = binding.buttonConnectToServer;
        btn.setVisibility(View.VISIBLE);
        btn.setEnabled(true);
        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        overridePendingTransition(0,0);
        finish();
    }
}