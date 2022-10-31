package com.BRS.baseball;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.BRS.ServerConnect.RequestHandle;

public class StartActivity extends AppCompatActivity {

    Button sideButton;
    Button behindButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);

        sideButton = findViewById(R.id.sideBtn);
        sideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestHandle.setCamera_position("side");
                startActivity(intent);
            }
        });

        behindButton = findViewById(R.id.behindBtn);
        behindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestHandle.setCamera_position("behind");
                startActivity(intent);
            }
        });
    }
}