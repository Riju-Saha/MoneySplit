package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.widget.ImageView


class MainActivity : ComponentActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerSwitch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 Check local session first
        if (SessionManager.isLoggedIn(this)) {
            val username = SessionManager.getLoggedInUsername(this)
            startActivity(Intent(this, Home::class.java).apply {
                putExtra("username", username)
            })
            finish()
        } else {
            setContentView(R.layout.login)

            usernameInput = findViewById(R.id.usernameInput)
            passwordInput = findViewById(R.id.passwordInput)
            loginButton = findViewById(R.id.loginButton)
            registerSwitch = findViewById(R.id.registerSwitch)

            val passwordInput = findViewById<EditText>(R.id.passwordInput)
            val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)

            var isPasswordVisible = false

            passwordToggle.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    passwordToggle.setImageResource(R.drawable.ic_eye_open) // eye open icon
                } else {
                    passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    passwordToggle.setImageResource(R.drawable.ic_eye_closed) // eye closed icon
                }
                passwordInput.setSelection(passwordInput.text.length)
            }

            loginButton.setOnClickListener {
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val dbHelper = FirebaseHelper()
                dbHelper.checkUser(username, password) { success ->
                    Log.d("LoginDebug", "Login attempt for username: $username, success: $success")
                    if (success) {
                        SessionManager.login(this, username)
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, Home::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("username", username)
                        })
                        finish()
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            registerSwitch.setOnClickListener {
                startActivity(Intent(this, Register::class.java))
            }
        }
    }
}
