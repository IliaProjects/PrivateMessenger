import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.function.BiConsumer;

public class RoomHandler {
    //private static Map<Integer, Room> roomsMap = new HashMap<>();

    private static Map<Integer, RoomHandler> roomHandlersMap = new HashMap<>();
    private HashMapMessages<Integer, Message> messagesMap = new HashMapMessages<>();
    private HashMapMembers<String, ClientHandler> membersMap = new HashMapMembers<>();
    private final int roomNr;
    private final String roomCreator;
    public final int maxMembers = 2;
    private Room room;


    //An extended HashMap class with listener for messages
    public static class HashMapMessages<K,V> extends HashMap<K,V> {
        private PropertyChangeSupport ps = new PropertyChangeSupport(this);

        // method to add listener
        public void addMapListener(PropertyChangeListener listener) {
            ps.addPropertyChangeListener(listener);
        }
        public void removeMapListener(PropertyChangeListener listener) {
            ps.removePropertyChangeListener(listener);
        }

        @Override
        public V put(K key, V value) {
            V ret = super.put(key,value);
            ps.firePropertyChange("messageput", key, value);
            return ret;
        }
    }
    //An extended HashMap class with listener for members
    public class HashMapMembers<K,V> extends HashMap<K,V> {

        private PropertyChangeSupport ps = new PropertyChangeSupport(this);

        // method to add listener
        public void addMapListener(PropertyChangeListener listener) {
            ps.addPropertyChangeListener(listener);
        }
        public void removeMapListener(PropertyChangeListener listener) {
            ps.removePropertyChangeListener(listener);
        }

        @Override
        public V put(K key, V value) {
            V ret = super.put(key,value);
            ps.firePropertyChange("memberput", key, value);
            return ret;
        }

        @Override
        public V remove(Object key) {
            V ret = super.remove(key);
            ps.firePropertyChange("memberremove", key, null);
            return ret;
        }
    }
    public static class Room {
        public final int nr;
        public final Date date;
        public final String creator;
        public final String companion;
        public Room(int mNr, String mCreator, String mCompanion) {
            this.nr = mNr;
            this.creator = mCreator;
            this.companion = mCompanion;
            this.date = new Date();
        }
    }
    public static class Message {
        public final String sender;
        public final Date date;
        public final String text;

        public Message(String mSender, String mText){
            date = new Date();
            sender = mSender;
            text = mText;
        }
    }

    public static synchronized RoomHandler getInstance(int key){
        //RoomHandler handler = roomHandlersMap.get(key);
        return roomHandlersMap.get(key);
    }

    //On click next button
    public static synchronized boolean roomHandlerExists(int roomNr){
        return RoomHandler.roomHandlersMap.containsKey(roomNr);
    }

    //On click enter room
    public static synchronized RoomHandler enterRoom(int key, String username, ClientHandler handler) throws IllegalAccessError, IllegalArgumentException, TooManyListenersException {
        RoomHandler roomHandler = roomHandlersMap.get(key);
        if(roomHandler == null || roomHandler.room == null){
            throw new IllegalArgumentException();
        }
        if(!roomHandler.room.companion.equals(username) && !roomHandler.room.creator.equals(username)) {
            throw new IllegalAccessError();
        }
        if(roomHandler.membersMap.size() > (roomHandler.maxMembers - 1)){
            throw new TooManyListenersException();
        }

        roomHandler.membersMap.put(username, handler);
        return roomHandler;
    }


    public RoomHandler(String mCreator, int mRoomNr, ClientHandler clientHandler)  {
        this.roomCreator = mCreator;
        this.roomNr = mRoomNr;
        this.membersMap.put(mCreator, clientHandler);
        roomHandlersMap.put(this.roomNr, this);
    }

    public synchronized Room createRoom(String companion) throws IllegalArgumentException{

        if(room != null) {
            throw new IllegalArgumentException();
        }

        this.room = new Room(this.roomNr, this.roomCreator, companion);
        return room;
    }

    public void leaveRoom(String key){
        this.membersMap.remove(key);
        if(membersMap.size() < 1) {
            RoomHandler.roomHandlersMap.remove(this.roomNr);
        }
    }

    public List<Message> getMessages(){
        List<Message> list = new ArrayList<Message>(messagesMap.values());
        return list;
    }

    public synchronized void sendMessage(String sender, String text){
        System.out.println("Room " + this.roomNr + " - " + sender + ":" + text);
        if(this.membersMap.containsKey(sender)) {
            try {
                messagesMap.put(messagesMap.size() + 1, new Message(sender, text));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //TODO данный метод не будет работать с конференциями >2чел
    public synchronized String getCompany(String username){
        String company = "";

        for(Map.Entry<String, ClientHandler> entry : membersMap.entrySet()) {
            String key = entry.getKey();
            if(!key.equals(username)){
                company = key;
            }
        }

        return company;
    }

    public Room getRoom() { return room; }

    public void addMessagesListener(PropertyChangeListener listener) {
        messagesMap.addMapListener(listener);
    }

    public void addMembersListener(PropertyChangeListener listener) {
        membersMap.addMapListener(listener);
    }

    public void removeMessagesListener(PropertyChangeListener listener) {
        messagesMap.removeMapListener(listener);
    }

    public void removeMembersListener(PropertyChangeListener listener) {
        membersMap.removeMapListener(listener);
    }
}