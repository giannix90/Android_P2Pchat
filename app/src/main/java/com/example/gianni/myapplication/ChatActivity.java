package com.example.gianni.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    TextView textbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        textbox=(TextView) findViewById(R.id.textView3);

        Intent intent = getIntent();
        String v = intent.getStringExtra("com.example.gianni.myapplication"); //if it's a string you stored

        textbox.append("\n"+v);



    }
}
