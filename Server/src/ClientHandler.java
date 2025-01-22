
// 1. Open a socket.
// 2. Open an input stream and output stream to the socket.
// 3. Read from and write to the stream according to the server's protocol.
// 4. Close the streams.
// 5. Close the socket.

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * When a client connects the server spawns a thread to handle the client.
 * This way the server can handle multiple clients at the same time.
 *
 * This keyword should be used in setters, passing the object as an argument,
 * and to call alternate constructors (a constructor with a different set of
 * arguments.
 */

// Runnable is implemented on a class whose instances will be executed by a thread.
public class ClientHandler implements Runnable, PropertyChangeListener {

    // Array list of all the threads handling clients so each message can be sent to the client the thread is handling.
    public static HashMap<String, ClientHandler> clientHandlers = new HashMap<>();
    // Id that will increment with each new client.

    // Socket for a connection, buffer reader and writer for receiving and sending data respectively.
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private String connectionTime;
    private RoomHandler roomHandler;
    Timer timer;

    private boolean clientDisconnected;


    public void extendSelfDisconnectDelay() {
        TimerTask selfDisconnectTask = new TimerTask() {
            @Override
            public void run() {
                onClientDisconnected(socket, bufferedReader, bufferedWriter);
            }
        };

        if(timer != null){
            timer.cancel();
            timer.purge();
        }
        timer = new Timer();
        timer.schedule(selfDisconnectTask, 600000);
    }


    private RoomHandler.HashMapMessages<Integer, RoomHandler.Message> unreadMessages = new RoomHandler.HashMapMessages<>();

    // Creating the client handler from the socket the server passes.
    public ClientHandler(Socket socket) {
        try {
            clientDisconnected = false;
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            int i = bufferedReader.read();
            if (i != -1) {
                String requestString = bufferedReader.readLine();
                Request request = new Request(requestString);

                String version = request.msg;
                String response;

                if (version.equals("1.00")) {
                    response = new ResponseBuilder().putResponse("VERSION_OK").build();
                    bufferedWriter.write(response);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

                    int connectionState = bufferedReader.read();
                    if (connectionState == -1) {
                        onClientDisconnected(socket, bufferedReader, bufferedWriter);
                    } else {
                        String loginRequestString = bufferedReader.readLine();
                        Request loginRequest = new Request(loginRequestString);
                        if (!"LOGIN".contains(loginRequest.head)) {
                            onClientDisconnected(socket, bufferedReader, bufferedWriter);
                        } else {
                            this.clientUsername = loginRequest.params[0].split("=")[1];

                            if (clientHandlers.containsKey(clientUsername)) {
                                response = new ResponseBuilder().putResponse("USERNAME_EXISTS").build();
                                System.out.println(clientUsername + " tried to connect!");
                            } else {
                                response = new ResponseBuilder().putResponse("LOGIN_OK").build();
                                this.connectionTime = Utils.dateToString(new Date());
                                clientHandlers.put(this.clientUsername, this);
                                System.out.println(clientUsername + " has connected!");
                            }
                            bufferedWriter.write(response);
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                    }
                } else {
                    response = new ResponseBuilder().putResponse("VERSION_ERROR").build();
                    bufferedWriter.write(response);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    onClientDisconnected(socket, bufferedReader, bufferedWriter);
                }
            } else {
                onClientDisconnected(socket, bufferedReader, bufferedWriter);
            }
        } catch (IOException e) {
            // Close everything more gracefully.
            onClientDisconnected(socket, bufferedReader, bufferedWriter);
        } catch (NullPointerException e) {
            onClientDisconnected(socket, bufferedReader, bufferedWriter);
        }
    }

    public String getClientUsername(){
        return clientUsername;
    }

    @Override
    public void run() {
        //ON USER LOGGED IN
        while (!socket.isClosed()) {
            try {
                extendSelfDisconnectDelay();
                int connectionState = bufferedReader.read();
                if (connectionState == -1){
                    onClientDisconnected(socket, bufferedReader, bufferedWriter);
                    return;
                }

                String requestString = bufferedReader.readLine();
                Request request = new Request(requestString);
                String response = new ResponseBuilder().useRequestErrorResponse().build();
                boolean getMessageFlag = false;
                switch (request.head) {
                    case "ENTER_ROOM":
                        response = enterRoomResponse(request);
                        break;

                    case "CREATE_ROOM":
                        response = createRoomResponse(request);
                        break;

                    case "CHECK_ROOM":
                        response = checkRoomResponse(request);
                        break;

                    case "LEAVE_ROOM":
                        response = leaveRoomResponse(request);
                        break;

                    case "GET_UNREAD_MESSAGES":
                        getMessageFlag = true;
                        pushMessagesToClient();
                        break;
                        //TODO подтверждение, что сообщение дошло
                    case "CHAT":
                        response = sendMessage(request);
                        break;

                    case "LOGOUT":
                        response = logoutResponse();
                        return;
                }
                if(!getMessageFlag) {
                    bufferedWriter.write(response);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

            } catch(IOException e){
                e.printStackTrace();
                onClientDisconnected(socket, bufferedReader, bufferedWriter);
            } catch (NullPointerException e){
                e.printStackTrace();
                onClientDisconnected(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void pushMessagesToClient() {
        if(this.roomHandler != null) {
            List<RoomHandler.Message> list = this.roomHandler.getMessages();
            new Thread(() -> {
                try {
                    Thread.sleep(750);
                    for (RoomHandler.Message message : list) {

                        String msgString = new ResponseBuilder().putResponse("NEW_MESSAGE").addParam("sender="+message.sender).addParam("time="+Utils.dateToString(message.date)).putMessage(message.text).build();

                        bufferedWriter.write(msgString);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    public String checkRoomResponse(Request request) {
        int nr;
        String result;
        try {
            nr = Integer.valueOf(request.msg);
        } catch (Exception e) {
            result = (new ResponseBuilder()).putResponse("INVALID_VALUE").build();
            return result;
        }
        //END validation

        if(RoomHandler.roomHandlerExists(nr)) {
            result = (new ResponseBuilder()).putResponse("ROOM_EXISTS").build();
            return result;
        }
        this.roomHandler = new RoomHandler(this.clientUsername, nr, this);
        result = (new ResponseBuilder()).putResponse("CHECK_ROOM_OK").build();
        return result;
    }

    public String createRoomResponse(Request request) {
        String companion = request.msg;
        String result;

        try {
            this.roomHandler.createRoom(companion);
        } catch (IllegalArgumentException ex) {
            result = (new ResponseBuilder()).putResponse("ROOM_EXISTS").build();
            return result;
        }

            this.roomHandler.addMessagesListener(this);
            this.roomHandler.addMembersListener(this);
            result = (new ResponseBuilder()).putResponse("OK").build();
            return result;
    }

    public String enterRoomResponse(Request request) {
        int nr;
        String result;
        try {
            nr = Integer.valueOf(request.msg);
        } catch (NumberFormatException e) {
            result = (new ResponseBuilder()).putResponse("INVALID_VALUE").build();
            return result;
        }
        //END validation

        RoomHandler handler;
        try {
            handler = RoomHandler.enterRoom(nr, clientUsername, this);
        } catch (TooManyListenersException e) {
            result = (new ResponseBuilder()).putResponse("ROOM_IS_FULL").build();
            return result;
        } catch (IllegalArgumentException e) {
            result = (new ResponseBuilder()).putResponse("ROOM_NOT_FOUND").build();
            return result;
        } catch (IllegalAccessError e){
            result = (new ResponseBuilder()).putResponse("ACCESS_DENIED").build();
            return result;
        }

        this.roomHandler = handler;
        String company = roomHandler.getCompany(clientUsername);
        handler.addMessagesListener(this);
        handler.addMembersListener(this);
        result = (new ResponseBuilder()).putResponse("ENTER_ROOM_OK").addParam("company="+company).build();
        System.out.println(clientUsername + " entered to " + company + " in room " + handler.getRoom());
        return result;
    }

    public String leaveRoomResponse(Request request) {
        if(roomHandler != null) {
            roomHandler.removeMessagesListener(this);
            roomHandler.removeMembersListener(this);
            roomHandler.leaveRoom(clientUsername);
            roomHandler = null;
        }
        String response = new ResponseBuilder().putResponse("LEAVE_ROOM_OK").build();
        return response;
    }
    public String sendMessage(Request request){
        this.roomHandler.sendMessage(clientUsername, request.msg);
        String response = new ResponseBuilder().putResponse("OK").build();
        return response;
    }

    public String logoutResponse() {
        onClientDisconnected(socket, bufferedReader, bufferedWriter);
        String response = new ResponseBuilder().putResponse("OK").build();
        return response;
    }
    // If the client disconnects for any reason remove them from the list so a message isn't sent down a broken connection.
    public synchronized void removeClientHandler() {
        clientHandlers.remove(this.clientUsername);
    }


    // Helper method to close everything so you don't have to repeat yourself.
    public void onClientDisconnected(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Note you only need to close the outer wrapper as the underlying streams are closed when you close the wrapper.
        // Note you want to close the outermost wrapper so that everything gets flushed.
        // Note that closing a socket will also close the socket's InputStream and OutputStream.
        // Closing the input stream closes the socket. You need to use shutdownInput() on socket to just close the input stream.
        // Closing the socket will also close the socket's input stream and output stream.
        // Close the socket after closing the streams.

        // The client disconnected or an error occurred so remove them from the list so no message is broadcasted.
        if(!clientDisconnected) {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (socket != null) {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                }
                if (roomHandler != null) {
                    this.roomHandler.removeMessagesListener(this);
                    this.roomHandler.removeMembersListener(this);
                    roomHandler.leaveRoom(clientUsername);
                }
                System.out.println(this.clientUsername + " DISCONNECTED!");
                clientDisconnected = true;
                removeClientHandler();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = "";

        if (!socket.isClosed()) {
            switch (evt.getPropertyName()) {
                case "memberremove":
                    String username = (String) evt.getOldValue();
                    if(this.clientUsername.equals(username)){
                        return;
                    }
                    event = new ResponseBuilder().putResponse("MEMBER_LEFT").addParam("username=" + username).build();
                    break;
                case "memberput":
                    String user = (String) evt.getOldValue();
                    if(this.clientUsername.equals(user)){
                        return;
                    }
                    event = new ResponseBuilder().putResponse("MEMBER_ENTERED").addParam("username=" + user).build();
                    break;
                case "messageput":
                    RoomHandler.Message message = (RoomHandler.Message) evt.getNewValue();
                    if(message.sender.equals(this.clientUsername)){
                        return;
                    }
                    unreadMessages.put((unreadMessages.size() + 1), message);
                    event = new ResponseBuilder().putResponse("NEW_MESSAGE").addParam("sender="+message.sender).addParam("time="+Utils.dateToString(message.date)).putMessage(message.text).build();
                    break;
            }
            try {
                bufferedWriter.write(event);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                onClientDisconnected(socket, bufferedReader, bufferedWriter);
                throw new RuntimeException(e);
            }
        }
        else {
            onClientDisconnected(socket, bufferedReader, bufferedWriter);
        }
    }
}