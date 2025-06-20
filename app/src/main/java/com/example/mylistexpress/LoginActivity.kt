package com.example.mylistexpress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val userNameInput = findViewById<EditText>(R.id.userNameInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val name = userNameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                val prefs = getSharedPreferences("MyListExpressPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("userName", name).apply()
                startActivity(Intent(this, ShoppingListActivity::class.java))
                finish()

            } else {
                Toast.makeText(this, "Por favor, ingresa tu nombre o apodo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}