package com.example.moneysplit

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerSwitch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 Check login session using SessionManager
        if (SessionManager.isLoggedIn(this)) {
            val username = SessionManager.getLoggedInUsername(this)
            val intent = Intent(this, Home::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
            finish()
        } else {
            setContentView(R.layout.login)

            usernameInput = findViewById(R.id.usernameInput)
            passwordInput = findViewById(R.id.passwordInput)
            loginButton = findViewById(R.id.loginButton)
            registerSwitch = findViewById(R.id.registerSwitch)

            // Copy database and log users
            copyDatabase(this, "moneySplit_db.db")
            useDatabase()

            loginButton.setOnClickListener {
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                val dbHelper = moneySplit_Database(this)

                if (dbHelper.checkUser(username, password)) {
                    // ✅ Save login using SessionManager
                    SessionManager.login(this, username)

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, Home::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("username", username)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }

            registerSwitch.setOnClickListener {
                val intent = Intent(this, Register::class.java)
                startActivity(intent)
            }
        }
    }

    fun copyDatabase(context: Context, dbName: String) {
        val dbPath = context.getDatabasePath(dbName)

        if (!dbPath.exists()) {
            dbPath.parentFile?.mkdirs()

            context.assets.open(dbName).use { inputStream ->
                FileOutputStream(dbPath).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.flush()
                }
            }
        }
    }

    fun useDatabase() {
        val db = SQLiteDatabase.openDatabase(
            getDatabasePath("moneySplit_db.db").path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )

        val cursor = db.rawQuery("SELECT * FROM users", null)

        if (cursor.moveToFirst()) {
            do {
                val column1 = cursor.getString(0)
                val column2 = cursor.getString(1)
                Log.d("DB_RESULT", "$column1 | $column2")
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
    }
}
