import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;


//////////////////   WEB SOCKET RESPONSE CLASS  //////////////////
//  - analyze the request
//  - send back handshake response
//  - keep listening the requests from client
//  - response to every requests, including: join a room, send message to a room, leave a room


public class WSResponse {

    // member variables
    private final DataInputStream dis_;
    private Room currentRoom_;
    public DataOutputStream outputStream_; // NEEDED IN THE ROOM CLASS: BROADCAST MESSAGES


    // STEP 1. constructor: create an Web Socket Response Object
    // assign the member variables
    public WSResponse(Socket socket, InputStream input){
        dis_ = new DataInputStream(input);
        currentRoom_ = null;
        outputStream_ = null;
        try {
            outputStream_ = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("An output stream error occurred: " + e.getMessage());
        }
    }


    // STEP 3. encode the socket key
    private synchronized String encode(String key){
        key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        byte[] asBytes = key.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("An error occurred when encoding web socket response key: " + e.getMessage());
        }
        assert md != null;
        md.update(asBytes);
        byte[] sha1 = md.digest();
        byte[] encodedBytes = Base64.getEncoder().encode(sha1);
        String encoded = new String(encodedBytes);
        return encoded;
    }


    // STEP 2. handle the handshake between server and client
    // send response to the client to finish the handshake process
    public synchronized void handShake(Map<String,String> requestMap){

        String webSocketKey = requestMap.get("Sec-WebSocket-Key");
        String acceptKey = encode(webSocketKey);

        assert outputStream_ != null;
        OutputStreamWriter stringWriter = new OutputStreamWriter(outputStream_);
        String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
        try {
            stringWriter.write(response, 0, response.length());
            stringWriter.flush();
        } catch (IOException ioe) {
            System.out.println("Handshake sending failed: " + ioe.getMessage());
        }

        System.out.println("Handshake succeeded...");
    }


    // STEP 6. handle the request
    // if the request is "join a room", put the client into the target room
    // if the request is "send a message", broadcast the message in the current room
    private void handleRequests(String payloadContent) {
        String[] request = payloadContent.split(" ", 3);
        if (request[0].equals("join")) {
            // create a room, or add the user into a room
            currentRoom_ = Room.getRoom(this, request[1], request[2]);
        } else if (request[0].equals("message")){
            // get the message content
            String username = request[1];
            String message = request[2];
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String time = formatter.format(date);
            // create a json
            String json = "{\"time\":\"" + time + "\",\"user\":\"" + username + "\",\"message\":\"" + message + "\"}";
            System.out.println("JSON: " + json);
            try {
                currentRoom_.broadcastMessages(json);
            } catch (IOException e) {
                System.out.println("A broadcasting error occurred: " + e.getMessage());
            }
        }else if (request[0].equals("requestUsersList")){
            currentRoom_.sendUsersList(this);
        }else if (request[0].equals("requestRoomList")){
            Room.sendRoomList(this);
        }
    }


    // STEP 5. read the request from the client
    // NOTE: if the client left the room, after opcode, the data input stream will meet an IOException
    // throw those exceptions to STEP4 keepListening() method, to break the while loop and end the thread
    private synchronized String readRequest() throws IOException {

        // read the first 2 bytes, figure out the length
        byte[] header = dis_.readNBytes(2);

        // opcode: 1 - text or 8 -client left
        short opcode = (short) (header[0] & 0x0F);
        if (opcode == 1) {
            System.out.println("This message is a text.");
        } else if (opcode == 8) {
            if (currentRoom_ != null) {
                currentRoom_.RemoveClient(this);
            }
//            System.out.println("The user is disconnected.");
        } else {
            System.out.println("Unable to recognize websocket opcode.");
        }

        // mask
        boolean mask = ((header[1] & 0x80) != 0);

        // payload length
        short length = (short) (header[1] & 0x7F);
        // determine how many more bytes to read for the length
        long furtherLength = 0;
        if (length == 126) {
            furtherLength = dis_.readUnsignedShort();
        } else if (length == 127) {
            furtherLength = dis_.readLong();
        }

        // read 4 bytes for the masking-key
        byte[] maskingKey = new byte[4];
        if (mask) {
            maskingKey = dis_.readNBytes(4);
        }

        // read the rest payload
        if (length <= 125) {
            byte[] payload = dis_.readNBytes(length);
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
            return new String(payload);
        }else if (length == 126){
            byte[] payload = dis_.readNBytes((int)furtherLength);
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
            return new String(payload);
        }else{
            byte[] payload1 = dis_.readNBytes(Integer.MAX_VALUE);
            byte[] payload2 = dis_.readNBytes((int)(furtherLength - Integer.MAX_VALUE));
            byte[] payload = new byte[payload1.length + payload2.length];
            for (int i = 0; i < payload.length; ++i)
            {
                payload[i] = i < payload1.length ? payload1[i] : payload2[i - payload1.length];
            }
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
            return new String(payload);
        }
    }


    // STEP 4. after the handshake, keep listening to the requests from the client
    // handle the requests, either join a room or send a message
    // catch exception when a client leaves a chat room
    public synchronized void keepListening() {
        while (true) {
            try {
                String payloadContent = readRequest();
                handleRequests(payloadContent);
            } catch (Exception e) {
                break;
            }
        }
    }
}