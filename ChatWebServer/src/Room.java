import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


//////////////////   ROOM CLASS  //////////////////
//  - create new rooms, or put clients to the existed rooms
//  - use a static array list to record the currently exist rooms
//  - record every room's status: how many of clients are currently in this room, and who are they
//  - broadcast messages to all the clients who are inside this room


public class Room {

    // member variables
    private static ArrayList<Room> roomsArr_ = new ArrayList<>();
    private Map<WSResponse, String> usersList_= new HashMap<>();
    private ArrayList<String> messagesArr_ = new ArrayList<>();
    private String roomName_ = "";


    // constructor
    private Room(WSResponse ws, String userName, String roomName) {
        roomName_ = roomName;
        usersList_.put(ws, userName);
        roomsArr_.add(this);
    }

    public synchronized static void sendRoomList(WSResponse ws) {
        StringBuilder roomList = new StringBuilder();
        for (Room room: roomsArr_){
            roomList.append(room.roomName_);
            roomList.append(" ");
        }
        assert !roomList.toString().equals("");
        String roomListFiltered = roomList.substring(0, roomList.length()-1);
        String json = "{ \"roomList\":\"" + roomListFiltered + "\"}";
        System.out.println(json);
        sendMessage(ws, json);
    }


    // remove a client from the current room
    public synchronized void RemoveClient(WSResponse ws) {
        usersList_.remove(ws);
        // if there is no users in this room anymore, remove this room
        if (usersList_.isEmpty()){
            roomsArr_.remove(this);
        }
        System.out.println("User has been removed from the current room.");
    }


    // get the current room for a client, if the room doesn't exist, create one
    public synchronized static Room getRoom(WSResponse ws, String userName, String roomName) {
        for (Room r : roomsArr_) {
            // if room already exists
            if (roomName.equals(r.roomName_)) {
                // add the user into this room
                r.usersList_.put(ws, userName);
                System.out.println("User " + userName + " joined the room: " + roomName);
                // send the client message history
                if (!r.messagesArr_.isEmpty()){
                    r.sendHistory(ws);
                }
                return r;
            }
        }
        // if room doesn't exist, create a room
        Room r = new Room(ws, userName, roomName);
        System.out.println("User joined the room: " + roomName);
        return r;
    }


    // send history messages in this room to the client when joins the room
    private synchronized void sendHistory(WSResponse user) {
        for (String json: messagesArr_){
            sendMessage(user, json);
        }
    }

    // send users' list who are currently in this room
    public synchronized void sendUsersList(WSResponse ws){
        String roomName = this.roomName_;
        StringBuilder usersList = new StringBuilder();
        for (WSResponse user: usersList_.keySet()){
            usersList.append(usersList_.get(user));
            usersList.append(" ");
        }
        assert !usersList.toString().equals("");
        String usersListFiltered = usersList.substring(0, usersList.length()-1);
        String json = "{ \"roomName\":\"" + roomName + "\",\"usersList\":\"" + usersListFiltered + "\"}";
        System.out.println(json);
        sendMessage(ws, json);
    }

    public static void sendMessage(WSResponse ws, String json){
        try {
            // output the 1st byte
            ws.outputStream_.write(0x81);
            // output the 2nd byte or more, depending on the length of json
            int payloadLength = json.length();
            if (payloadLength <= 125) {
                ws.outputStream_.write(payloadLength);
            } else if (payloadLength < Short.MAX_VALUE * 2) {
                ws.outputStream_.write(126);
                ws.outputStream_.writeShort(payloadLength);
            } else {
                ws.outputStream_.write(127);
                ws.outputStream_.writeLong(payloadLength);
            }
            ws.outputStream_.write(json.getBytes());
            ws.outputStream_.flush();
        } catch (IOException e) {
            System.out.println("An error occurred when trying to send back message to client: " + e.getMessage());
        }
    }

    // if any of the clients inside a room sends a message
    // broadcast this message to every client that is in this room
    public void broadcastMessages(String json) throws IOException {
        messagesArr_.add(json);
        for (WSResponse user: usersList_.keySet()) {
            sendMessage(user, json);
        }
    }
}