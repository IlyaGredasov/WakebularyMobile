package com.example.wakebulary.frontend

import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.example.wakebulary.R
import com.example.wakebulary.backend.DataBaseClient
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)
    }
}