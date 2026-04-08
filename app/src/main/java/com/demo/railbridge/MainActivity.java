package com.demo.railbridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.railbridge.kotlin.KotlinWebViewActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startJavaDemoButton = findViewById(R.id.btnStartJavaDemo);
        startJavaDemoButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            startActivity(intent);
        });

        Button startKotlinDemoButton = findViewById(R.id.btnStartKotlinDemo);
        startKotlinDemoButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KotlinWebViewActivity.class);
            startActivity(intent);
        });
    }
}
