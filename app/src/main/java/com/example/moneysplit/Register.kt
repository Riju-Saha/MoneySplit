package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity

class Register : ComponentActivity() {

    private lateinit var nameNewInput: EditText
    private lateinit var usernameNewInput: EditText
    private lateinit var passwordNewInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginSwitch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        nameNewInput = findViewById(R.id.nameNewInput)
        usernameNewInput = findViewById(R.id.usernameNewInput)
        passwordNewInput = findViewById(R.id.passwordNewInput)
        registerButton = findViewById(R.id.registerButton)
        loginSwitch = findViewById(R.id.loginSwitch)

        registerButton.setOnClickListener {
            val fullname = nameNewInput.text.toString().trim()
            val username = usernameNewInput.text.toString().trim()
            val password = passwordNewInput.text.toString().trim()

            if (fullname.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                val dbHelper = moneySplit_Database(this)
                val success = dbHelper.insertUser(fullname, username, password)

                if (success) {
                    // ✅ Save session using SessionManager
                    SessionManager.saveUserSession(this, username)

                    val intent = Intent(this, CardDetailsInput::class.java).apply {
                        putExtra("username", username)
                        putExtra("fromProfile", false)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        loginSwitch.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

}
