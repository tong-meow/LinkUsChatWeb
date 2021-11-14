package com.example.chatapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity {


    // member variables: user's name + room's name
    // the values will be passed to the next page: ChatActivity
    private EditText userName_;
    private EditText roomName_;


    // override the onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set the variables for views on the current page
        Button enterRoomBtn = findViewById(R.id.enterRoom);
        userName_ = findViewById(R.id.userName);
        roomName_ = findViewById(R.id.roomName);

        // set a click listener to the 'enter room' button
        enterRoomBtn.setOnClickListener(new View.OnClickListener() {  // why is this grey?
            @Override
            public void onClick(View v) { // what is this view v?
                enterARoom();
            }
        });
    }


    // method for checking username & room name's validation
    // and set the intent, pass these 2 values to the ChatActivity
    public void enterARoom() {
        // check if the user entered a valid user name
        // if not, stop move to the chat activity, change the hint info
        boolean validInput = true;
        String userName = userName_.getText().toString();
        String roomName = roomName_.getText().toString();
        if (roomName.equals("") || userName.equals("")){
            validInput = false;
        }
        for (int i = 0; i < userName.length(); i++) {
            if (userName.charAt(i) == ' '){
                System.out.println("Users entered an invalid user name...");
                userName_.setText("");
                userName_.setHint("no space allowed");
                validInput = false;
                break;
            }
        }
        // check if the user entered a valid room name
        // if not, stop move to the chat activity, change the hint info
        for (int i = 0; i < roomName.length(); i++) {
            if (roomName.charAt(i) < 'a' || roomName.charAt(i) > 'z') {
                System.out.println("Users entered an invalid room name...");
                roomName_.setText("");
                roomName_.setHint("lowercase required");
                validInput = false;
                break;
            }
        }
        // if valid, move to the chat activity
        if (validInput) {
            System.out.println("User " + userName_.getText().toString() + " enters the room: " + roomName);
            Intent chatIntent = new Intent(this, ChatActivity.class);
            // pass the userName and roomName to the chat activity
            chatIntent.putExtra("userName", userName);
            chatIntent.putExtra("roomName", roomName);
            startActivity(chatIntent);
        }
    }


}