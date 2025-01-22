package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.messenger.databinding.FragmentMainEnterRoomBinding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class EnterRoomMainFragment extends Fragment {

    private FragmentMainEnterRoomBinding binding;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    MainActivity mainActivityContext;
    EditText enterRoomInput;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainEnterRoomBinding.inflate(inflater, container, false);
        Socket socket = SocketHandler.getSocket();
        enterRoomInput = binding.enterRoomInput;
        mainActivityContext = (MainActivity)getActivity();
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if(bufferedReader.markSupported()){
                bufferedReader.mark(8192);
            }
        } catch (IOException e) {
            onDisconnected();
            e.printStackTrace();
        }
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EnterRoomMainFragment fragment = EnterRoomMainFragment.this;

        //Focus on input & show keyboard
        enterRoomInput.requestFocus();
        InputMethodManager imm = (InputMethodManager)  getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(enterRoomInput, InputMethodManager.SHOW_IMPLICIT);

        try {
            bufferedReader.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        binding.enterExistingRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nr = enterRoomInput.getText().toString();
                if (nr.length() < 1) {
                    if(!toastBlocked) {
                        delayToast(2);
                        Toast.makeText(getContext(), "Введите номер комнаты", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                mainActivityContext.showProgressOverlay();

                String request = new RequestBuilder().putRequest("ENTER_ROOM").putMessage(nr).build();
                (new Thread(new RequestRunnable(request,
                    result -> {
                        Response response = new Response(result);
                        readBufferLoop(response);
                    },
                    () -> {
                        onDisconnected();
                    }
                ))).start();
            }
        });

        binding.createNewRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NavHostFragment.findNavController(fragment).navigate(R.id.action_EnterRoomMainFragment_to_CreateRoomMainFragment);
                        mainActivityContext.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                });
            }
        });

        binding.textUserMainActivity.setText(User.getMyUsername());
        binding.textUserMainActivity.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setClipboard(mainActivityContext, binding.textUserMainActivity.getText().toString());
                Toast.makeText(mainActivityContext, "Скопировано", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        binding.buttonLogout.setOnClickListener(v -> {
            mainActivityContext.logout();
        });
    }
    private void setClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    public void onDisconnected() {
        Intent intent = new Intent(mainActivityContext, StartActivity.class);
        startActivity(intent);
        mainActivityContext.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        mainActivityContext.finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //TODO сделать общий сервис для хранения стека запросов!!!!!!!!!!!!!
    public void readBufferLoop(Response response) {
        mainActivityContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (response.head){
                    case "ENTER_ROOM_OK":
                        String nr = enterRoomInput.getText().toString();
                        String company = response.params[0].split("=")[1];
                        Bundle bundle = new Bundle();
                        bundle.putString("company", company);
                        bundle.putInt("roomNr", Integer.valueOf(nr));
                        bundle.putBoolean("roomCreated", false);
                        Intent intent = new Intent(mainActivityContext, ChatActivity.class);
                        intent.putExtras(bundle);
                        mainActivityContext.hideProgressOverlay();
                        mainActivityContext.startActivity(intent);
                        mainActivityContext.finish();
                        break;
                    case "INVALID_VALUE":
                        if(!toastBlocked) {
                            delayToast(2);
                            Toast.makeText(mainActivityContext, "Ошибка запроса", Toast.LENGTH_SHORT).show();
                        }
                        mainActivityContext.hideProgressOverlay();
                        break;
                    case "ACCESS_DENIED":
                        if(!toastBlocked) {
                            delayToast(2);
                            Toast.makeText(mainActivityContext, "У вас нет доступа к этой комнате", Toast.LENGTH_SHORT).show();
                        }
                        mainActivityContext.hideProgressOverlay();
                        break;
                    case "ROOM_IS_FULL":
                        if(!toastBlocked) {
                            delayToast(2);
                            Toast.makeText(mainActivityContext, "Комната занята", Toast.LENGTH_SHORT).show();
                        }
                        mainActivityContext.hideProgressOverlay();
                        break;
                    case "ROOM_NOT_FOUND":
                        Toast.makeText(mainActivityContext,"Комната еще не открыта", Toast.LENGTH_SHORT).show();
                        mainActivityContext.hideProgressOverlay();
                        break;
                    default:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    int state = bufferedReader.read();
                                    if (state != -1) {
                                        Response newResponse = new Response(bufferedReader.readLine());
                                        readBufferLoop(newResponse);
                                    } else {
                                        onDisconnected();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    onDisconnected();
                                } catch (NullPointerException e) {
                                    onDisconnected();
                                }
                            }
                        }).start();
                        break;
                }
            }
        });
    }
}
