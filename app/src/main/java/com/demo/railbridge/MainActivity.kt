package com.demo.railbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.demo.railbridge.kotlin.KotlinWebViewActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startJavaDemoButton = findViewById<Button>(R.id.btnStartJavaDemo)
        startJavaDemoButton.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }

        val startKotlinDemoButton = findViewById<Button>(R.id.btnStartKotlinDemo)
        startKotlinDemoButton.setOnClickListener {
            startActivity(Intent(this, KotlinWebViewActivity::class.java))
        }
    }
}
