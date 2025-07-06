package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewProfile : AppCompatActivity() {

    private lateinit var dbHelper: moneySplit_Database
    private lateinit var nameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var saveButton: Button
    private lateinit var addCardButton: Button
    private lateinit var cardsRecyclerView: RecyclerView

    private var currentUsername: String? = null
    private var storedPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_profile)
        supportActionBar?.hide()

        dbHelper = moneySplit_Database(this)

        nameInput = findViewById(R.id.nameEditText)
        usernameInput = findViewById(R.id.usernameEditText)
        passwordInput = findViewById(R.id.passwordEditText)
        saveButton = findViewById(R.id.saveProfileButton)
        addCardButton = findViewById(R.id.addCardButton)
        cardsRecyclerView = findViewById(R.id.cardsRecyclerView)

        currentUsername = SessionManager.getLoggedInUsername(this)

        if (currentUsername != null) {
            val user = dbHelper.getUserByUsername(currentUsername!!)
            if (user != null) {
                nameInput.setText(user.name)
                usernameInput.setText(user.username)
                passwordInput.setText("")
                storedPassword = user.password
            }
        }

        setupCardsSection()

        saveButton.setOnClickListener {
            val newName = nameInput.text.toString().trim()
            val newUsername = usernameInput.text.toString().trim()
            val enteredPassword = passwordInput.text.toString().trim()

            if (newName.isEmpty() || newUsername.isEmpty() || enteredPassword.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (enteredPassword != storedPassword) {
                Toast.makeText(this, "Incorrect password. Update failed.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.updateUser(currentUsername!!, newName, newUsername, storedPassword!!)) {
                SessionManager.saveUserSession(this, newUsername)
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                currentUsername = newUsername

                val intent = Intent(this, Home::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("username", currentUsername)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }

        addCardButton.setOnClickListener {
            val intent = Intent(this, CardDetailsInput::class.java).apply {
            putExtra("username", currentUsername)
            putExtra("fromProfile", true)
            }
            startActivity(intent)
        }
    }

    private fun setupCardsSection() {
        cardsRecyclerView.layoutManager = LinearLayoutManager(this)
        loadCards()
    }

    private fun loadCards() {
        val cards = dbHelper.getCardsForUser(currentUsername!!)
        val adapter = CardTableAdapter(cards) { card ->
            AlertDialog.Builder(this)
                .setTitle("Delete Card")
                .setMessage("Are you sure you want to delete this card?")
                .setPositiveButton("Yes") { _, _ ->
                    dbHelper.deleteCard(card.id, currentUsername!!)
                    Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
                    loadCards()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        cardsRecyclerView.adapter = adapter
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("username", currentUsername)
        startActivity(intent)
        finish()
    }

}
