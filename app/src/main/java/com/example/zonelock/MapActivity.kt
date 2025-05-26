package com.example.zonelock

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val btnBack = findViewById<Button>(R.id.btn_back_to_admin)
        btnBack.setOnClickListener {
            finish() // 이전 액티비티(AdminActivity)로 돌아감
        }
    }
}