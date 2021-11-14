package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    // member variables
    private WebSocket webSocket_;
    private String userName_;
    private String roomName_;
    private ListView list_;
//    private ArrayList<String> messages_;   // BUT WHY??
    private ArrayAdapter<String> adapter_;


    @Override
    protected void onCreate(Bundle savedInstanceState) { // this is a UI thread
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        /////////// ASSIGN & SET SOME BASIC VARIABLES //////////
        // set the room name
        TextView roomTitle = findViewById(R.id.roomTitle);
        Bundle extras = getIntent().getExtras();
        if (extras != null ) {
            userName_ = extras.getString("userName");
            roomName_ = extras.getString("roomName");
            roomTitle.setText(String.format("ChatRoom #%s", roomName_));
        }
        Log.d("CC:ChatActivity", "User" + userName_ + " enter the room: " + roomName_);
        // create an array list to store messages received
        ArrayList<String> messages = new ArrayList<>();


        /////////// CREATE, OPEN A WEBSOCKET, AND DEALING WITH EVENTS //////////
        // Create a WebSocketFactory instance.
        WebSocketFactory factory = new WebSocketFactory();

        // Create a WebSocket. The timeout value set above is used.
        try {
            webSocket_ = factory.createSocket("ws://10.0.2.2:8080/hi");
        } catch (IOException e) {
            System.out.println("An error occurred when creating a web socket: " + e.getMessage());
        }

        // handle webSocket on connect
        webSocket_.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers){
                Log.d("CC:ChatActivity", "WebSocket handshake succeed.");
                // create the json for 'join room'
                String joinRoomJson = "join " + userName_ + " " + roomName_;
                webSocket_.sendText(joinRoomJson);
            }
        });

        // handle received messages from the server through webSocket
        webSocket_.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String message){
                Log.d("CC:ChatActivity", "Received a message: " + message);
                ArrayList<String> messageText = destructJson(message);
                messages.addAll(messageText);
                handleMessages();
            }
        });

        // handle webSocket errors
        webSocket_.addListener(new WebSocketAdapter() {
            @Override
            public void onError(WebSocket websocket, WebSocketException cause){
                Log.d("CC:ChatActivity", "Error occurred: " + cause.getMessage());
            }
        });

        // handle webSocket when server closed
        webSocket_.addListener(new WebSocketAdapter() {
            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                       WebSocketFrame clientCloseFrame, boolean closedByServer){

                TextView enterText = findViewById(R.id.textpad);
                enterText.setText("Server Closed");
                enterText.setEnabled(false);
            }
        });

        // connect to the server and perform an opening handshake
        try {
            webSocket_.connectAsynchronously();
        } catch (Exception e) {
            System.out.println("An unexpected error occurred in handshake.");
        }


        /////////// START DEALING WITH MESSAGES IN AND OUT //////////
        // set up the message list view and adapter
        list_ = findViewById(R.id.chatList);
        adapter_ = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages);

        // set the listener for the 'send' button
        // once the user click 'send' button:
        //  - send the message to the server
        //  - reset the text bar to empty, to let the user send the next message
        Button sendBtn = findViewById(R.id.sendBtn);
        TextView message = findViewById(R.id.textpad);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {  // what is this view v?
                String messageText = message.getText().toString();
                Log.d("CC:ChatActivity", messageText);
                sendMessage(messageText);
                message.setText("");
            }
        });
    }


    ///////////// OTHER METHODS ///////////////
    // destruct JSON
    // don't know why the GSON doesn't work T-T
    // have to do it with a hard code way
    private ArrayList<String> destructJson(String message) {
        ArrayList<String> strings = new ArrayList<>();
//        String messageText = "";
        String[] pieces = message.split("\"");
        if (pieces[1].equals("time")){
            strings.add("[ " + pieces[3] + " ] " + pieces[7] + ": " + pieces[11]);
        }else if (pieces[1].equals("roomName")){
            // display the users
            strings.add(">>>> Users in current room: ");
            String[] users = pieces[7].split(" ");
            for (String user: users) {
                strings.add("* " + user);
            }
        }else if (pieces[1].equals("roomList")){
            // display the rooms
            strings.add(">>>> Current available rooms: ");
            String[] rooms = pieces[3].split(" ");
            for (String room: rooms) {
                strings.add("* " + room);
            }
        }
        return strings;
    }


    // using Array Adapter to display the messages on the list view
    private void handleMessages() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("CC:ChatActivity", "start handle message");
                list_.setAdapter(adapter_);
                // this: this is built into the program
                list_.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter_.notifyDataSetChanged();
                        list_.smoothScrollToPosition(adapter_.getCount());
                    }
                });
            }
        });
    }


    // chat client send messages to the server
    private void sendMessage(String messageText) {
        String messageJson = "message " + userName_ + " " + messageText;
        webSocket_.sendText(messageJson);
    }


    ////////////////// MENU /////////////////////

    // add menu item
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chatmenu, menu);
        return true;
    }

    // menu item
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.roomInfo:
                // send request for roomInfo
                String requestInfo = "requestUsersList " + userName_ + roomName_;
                webSocket_.sendText(requestInfo);
                return true;
            case R.id.roomList:
                // send request for roomList
                String requestRoom = "requestRoomList " + userName_ + roomName_;
                webSocket_.sendText(requestRoom);
                return true;
            case R.id.about:
                // send request for about page
                Intent aboutpage = new Intent(this, AboutActivity.class);
                aboutpage.putExtra("userName", userName_);
                aboutpage.putExtra("roomName", roomName_);
                startActivity(aboutpage);
                return true;
            case R.id.logout:
                // back to the login page
                Intent homepage = new Intent(this, MainActivity.class);
                startActivity(homepage);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}