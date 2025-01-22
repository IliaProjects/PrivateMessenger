package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.messenger.databinding.ActivityChatBinding;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.ParseException;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityChatBinding binding;
    private Integer roomNr;

    private User me;
    private User companion;

    ImageButton btnSend;
    ImageButton btnAttachment, btnImage;
    EditText inputField;
    ImageView statusIndicator;
    TextView roomNrTextView;
    TextView companionUsernameTextview;

    Context context;

    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    Socket socket;

    MessagesListAdapter<Message> adapter;
    private MessagesList messagesListView;
    private boolean isBackPressed;
    private boolean loopStarted;

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
    int cnt = 0; //Sets message counter id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isBackPressed = false;
        loopStarted = false;
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        statusIndicator = binding.statusIndicator;
        roomNrTextView = binding.roomNrText;
        companionUsernameTextview = binding.companyUsername;

        try {
            socket = SocketHandler.getSocket();
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        me = new User(User.getMyUsername());
        Bundle b = getIntent().getExtras();

        roomNr = b.getInt("roomNr");
        roomNrTextView.setText(String.valueOf(roomNr));

        String companionUsername = b.getString("company");
        companion = new User(companionUsername);
        companionUsernameTextview.setText(companionUsername);


        Boolean roomCreated = b.getBoolean("roomCreated");
        if(!roomCreated) {
            statusIndicator.setBackground(getDrawable(R.drawable.indicator_online));
        }

        context = this;

        adapter = new MessagesListAdapter<>(User.getMyUsername(), null );
        messagesListView = binding.chatMessagesListView;
        messagesListView.setAdapter(adapter);

        inputField = binding.etMessage;
        btnSend = binding.btSend;
        btnImage = binding.btImage;
        btnAttachment = binding.btAttachment;

        this.messagesListView = binding.chatMessagesListView;

        btnSend.setEnabled(true);
        setSupportActionBar(binding.toolbar);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = inputField.getText().toString();
                if (messageText.length() > 0) {
                    String request = new RequestBuilder().putRequest("CHAT").putMessage(messageText).build();
                    new Thread(new RequestRunnable(request)).start();

                    Message message = new Message(me.getId(), me, messageText);
                    adapter.addToStart(message,true);
                    inputField.setText("");
                }
            }
        });

        btnAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!toastBlocked) {
                    delayToast(2);
                    Toast.makeText(context, "Обмен файлами будет доступен в следующей версии", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!toastBlocked) {
                    delayToast(2);
                    Toast.makeText(context, "Обмен файлами будет доступен в следующей версии", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.buttonBack.setOnClickListener(v -> {
            if(!isBackPressed){
                isBackPressed = true;
                String request = new RequestBuilder().putRequest("LEAVE_ROOM").build();
                (new Thread(new RequestRunnable(request))).start();
                Intent intent = new Intent(this, MainActivity.class);
                context.startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!loopStarted) {
            loopStarted = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String request = new RequestBuilder().putRequest("GET_UNREAD_MESSAGES").build();
                        bufferedWriter.write(request);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        readBufferLoop();
                    } catch (IOException e) {
                        e.printStackTrace();
                        onDisconnected();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed() {
        if(!isBackPressed) {
            isBackPressed = true;
            String request = new RequestBuilder().putRequest("LEAVE_ROOM").build();
            (new Thread(new RequestRunnable(request))).start();
            Intent intent = new Intent(this, MainActivity.class);
            context.startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onDisconnected() {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void readBufferLoop() throws IOException {
        Context context = this;
        int connectionState = bufferedReader.read();
        if (connectionState != -1) {
            String eventString = bufferedReader.readLine();
            if(eventString != null) {
                Response event = new Response(eventString);
                switch (event.head) {
                    case "MEMBER_LEFT":
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Участник вышел", Toast.LENGTH_SHORT).show();
                            statusIndicator.setBackground(getDrawable(R.drawable.indicator_offline));
                        });
                        break;
                    case "MEMBER_ENTERED":
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Участник присоединился", Toast.LENGTH_SHORT).show();
                            statusIndicator.setBackground(getDrawable(R.drawable.indicator_online));
                        });
                        break;
                    case "NEW_MESSAGE":
                        String username = event.params[0].split("=")[1];
                        String dateString = event.params[1].split("=")[1];
                        Date date = null;
                        try {
                            date = Utils.stringToDate(dateString);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        User user;
                        if (me.getName().equals(username)) {
                            user = me;
                        } else {
                            user = companion;
                        }
                        Message message = new Message(user.getId(), user, event.msg, date);
                        runOnUiThread(() -> {
                            adapter.addToStart(message, true);
                            inputField.setText("");
                        });
                        break;
                    default:
                        break;
                    case "LEAVE_ROOM_OK":
                        return;
                }
            }
            readBufferLoop();
        } else {
            onDisconnected();
        }
    }
}