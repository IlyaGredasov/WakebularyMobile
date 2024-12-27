package com.example.wakebulary.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.util.Log
import com.example.wakebulary.backend.DataBaseClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val client = DataBaseClient(applicationContext)
            client.insertTranslation("рус", listOf("abc","cde"))
            val d = client.translateWord("рус")
        }
        catch (e: Exception) {
            e.message?.let { Log.i("Print", it) }
        }
    }
}