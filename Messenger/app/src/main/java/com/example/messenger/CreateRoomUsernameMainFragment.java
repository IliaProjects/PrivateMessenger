package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.messenger.databinding.FragmentMainCreateRoomUsernameBinding;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class CreateRoomUsernameMainFragment extends BaseFragment {

    private FragmentMainCreateRoomUsernameBinding binding;
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private boolean enteringRoomFlag = false;
    private Integer roomNr;
    MainActivity mainActivityContext;

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
        binding = FragmentMainCreateRoomUsernameBinding.inflate(inflater, container, false);
        socket = SocketHandler.getSocket();
        roomNr = getArguments().getInt("room");
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            onDisconnected();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CreateRoomUsernameMainFragment fragment = CreateRoomUsernameMainFragment.this;

        mainActivityContext = (MainActivity)getActivity();
        //Focus on input & show keyboard
        EditText companionUsernameInput = binding.companionUsernameInput;
        companionUsernameInput.requestFocus();
        InputMethodManager imm = (InputMethodManager)  getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(companionUsernameInput, InputMethodManager.SHOW_IMPLICIT);

        companionUsernameInput.requestFocus();
        binding.enterNewRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String companionUsername = companionUsernameInput.getText().toString();
                if (companionUsername.length() < 3) {
                    if(!toastBlocked) {
                        delayToast(2);
                        Toast.makeText(getContext(), "Имя собеседника должно состоять не менее, чем из 3-х символов", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                mainActivityContext.showProgressOverlay();
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Validation
                        String request = new RequestBuilder().putRequest("CREATE_ROOM").putMessage(companionUsername).build();

                        (new Thread(new RequestRunnable(request, result -> {
                            Response response = new Response(result);
                            switch (response.head){
                                case "OK":
                                    enteringRoomFlag = true;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("company", companionUsername);
                                    bundle.putInt("roomNr", roomNr);
                                    bundle.putBoolean("roomCreated", true);
                                    Intent intent = new Intent(mainActivityContext, ChatActivity.class);
                                    intent.putExtras(bundle);
                                    mainActivityContext.startActivity(intent);
                                    mainActivityContext.finish();
                                    break;
                                case "ROOM_EXISTS":
                                    mainActivityContext.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mainActivityContext,"Ошибка создания комнаты", Toast.LENGTH_SHORT).show();

                                        }});
                                    break;
                                default:
                                    mainActivityContext.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mainActivityContext,"Ошибка", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    break;
                            }
                            mainActivityContext.hideProgressOverlay();
                        }, () -> {
                            onDisconnected();
                        }))).start();
                    }
                })).start();
            }
        });

        binding.questionButton.setOnClickListener(v -> {

            if(!toastBlocked) {
                delayToast(4);

                Toast.makeText(mainActivityContext, "Имя пользователя собеседника, которому будет открыт доступ в конференцию. В конференцию будете иметь доступ только вы и ваш собеседник", Toast.LENGTH_LONG).show();
            }});

        MaterialTextView textView = binding.textviewRoomnr;
        textView.setText(String.valueOf(roomNr));
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setClipboard(mainActivityContext, binding.textviewRoomnr.getText().toString());
                Toast.makeText(mainActivityContext, "Скопировано", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        binding.buttonBack.setOnClickListener(v -> {
            NavHostFragment.findNavController(fragment).navigate(R.id.action_CreateRoomUsernameMainFragment_to_CreateRoomMainFragment);
        });
    }
    private void setClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    public void onDisconnected() {
        Intent intent = new Intent(getContext(), StartActivity.class);
        startActivity(intent);
        mainActivityContext.finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //TODO вызывать leave room только когда возвращаемся
        if(!enteringRoomFlag) {
            String request = new RequestBuilder().putRequest("LEAVE_ROOM").build();
            (new Thread(new RequestRunnable(request, result -> { }, () -> { }))).start();
        }
        binding = null;
    }


}