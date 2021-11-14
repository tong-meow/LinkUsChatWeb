package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class AboutActivity extends AppCompatActivity {

    private String userName_;
    private String roomName_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Bundle extras = getIntent().getExtras();
        if (extras != null ) {
            userName_ = extras.getString("userName");
            roomName_ = extras.getString("roomName");
        }

    }

    // add menu item
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.aboutmenu, menu);
        return true;
    }

    // menu item
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.back:
                // go back to the chat room page
                Intent chatpage = new Intent(this, ChatActivity.class);
                chatpage.putExtra("userName", userName_);
                chatpage.putExtra("roomName", roomName_);
                startActivity(chatpage);
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