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

import com.example.messenger.databinding.FragmentMainCreateRoomBinding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

public class CreateRoomMainFragment extends Fragment {

    private FragmentMainCreateRoomBinding binding;
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    MainActivity mainActivityContext;
    CreateRoomMainFragment fragment;
    EditText createRoomNrInput;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        binding = FragmentMainCreateRoomBinding.inflate(inflater, container, false);
        socket = SocketHandler.getSocket();
        mainActivityContext = (MainActivity)getActivity();
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
        fragment = CreateRoomMainFragment.this;
        binding.createRoomNrInput.requestFocus();

        binding.createRoomRandomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rand = ThreadLocalRandom.current().nextInt(1, 999);
                binding.createRoomNrInput.setText(String.valueOf(rand));
            }
        });


        //Focus on input & show keyboard
        createRoomNrInput = binding.createRoomNrInput;
        createRoomNrInput.requestFocus();
        InputMethodManager imm = (InputMethodManager)  getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(createRoomNrInput, InputMethodManager.SHOW_IMPLICIT);

        binding.createRoomNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomNr = createRoomNrInput.getText().toString();
                if (roomNr.length() < 1) {
                    if(!toastBlocked){
                        delayToast(2);
                        Toast.makeText(getContext(), "Введите номер комнаты", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                mainActivityContext.showProgressOverlay();

                String request = new RequestBuilder().putRequest("CHECK_ROOM").putMessage(roomNr).build();
                (new Thread(new RequestRunnable(request, result -> {
                        Response response = new Response(result);
                        readBufferLoop(response, roomNr);
                    },
                    () -> {
                        onDisconnected();
                    }))).start();
            }
        });

        binding.usernameTextview.setText(User.getMyUsername());
        binding.usernameTextview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setClipboard(mainActivityContext, binding.usernameTextview.getText().toString());
                Toast.makeText(mainActivityContext, "Скопировано", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        binding.buttonBack.setOnClickListener(v -> {
            NavHostFragment.findNavController(fragment).navigate(R.id.action_CreateRoomMainFragment_to_EnterRoomMainFragment);
        });
    }

    public void onDisconnected(){
        Intent intent = new Intent(mainActivityContext, StartActivity.class);
        startActivity(intent);
        mainActivityContext.finish();
    }

    private void setClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void readBufferLoop(Response response, String nr){
        Log.e("Result checking room: ", response.head);
        mainActivityContext.runOnUiThread(() -> {
            switch (response.head){
                case "CHECK_ROOM_OK":
                    Bundle bundle = new Bundle();
                    bundle.putInt("room", Integer.valueOf(nr));
                    NavHostFragment.findNavController(fragment).navigate(R.id.action_CreateRoomMainFragment_to_CreateRoomUsernameMainFragment, bundle);
                    mainActivityContext.hideProgressOverlay();
                    break;
                case "INVALID_VALUE":
                    Toast.makeText(mainActivityContext,"Ошибка", Toast.LENGTH_SHORT).show();
                    mainActivityContext.hideProgressOverlay();
                    break;
                case "ROOM_EXISTS":
                    if(!toastBlocked) {
                        delayToast(2);
                        Toast.makeText(mainActivityContext, "Такая комната уже сущетсвует", Toast.LENGTH_SHORT).show();
                    }
                    mainActivityContext.hideProgressOverlay();
                    break;
                default:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int state = bufferedReader.read();
                                if (state != -1) {
                                    String responseString = bufferedReader.readLine();
                                    Response newResponse = new Response(responseString);
                                    readBufferLoop(newResponse, nr);
                                } else {
                                    onDisconnected();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                onDisconnected();
                            } catch (NullPointerException e) {
                                Log.e("NULLPOINTER REACHED!!","");
                                onDisconnected();
                            }
                        }
                    }).start();
                    break;
            }
        });
    }
}