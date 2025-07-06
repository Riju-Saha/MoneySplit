package com.example.moneysplit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CardDetailsInput : AppCompatActivity() {

    private lateinit var typeSpinner: Spinner
    private lateinit var last4Input: EditText
    private lateinit var addButton: Button
    private lateinit var whyIcon: ImageView
    private lateinit var whyText: TextView
    private lateinit var skipButton: Button

    private var username: String? = null

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_details_input)
        supportActionBar?.hide()

        typeSpinner = findViewById(R.id.typeSpinner)
        last4Input = findViewById(R.id.last4Input)
        addButton = findViewById(R.id.addDigitsButton)
        whyIcon = findViewById(R.id.whyIcon)
        whyText = findViewById(R.id.whyText)
        skipButton = findViewById(R.id.skipButton)

        username = intent.getStringExtra("username")

        val options = arrayOf("Select Type", "Debit Card", "Credit Card", "Account Number")
        val adapter = ArrayAdapter(this, R.drawable.spinner_item, options)
        adapter.setDropDownViewResource(R.drawable.spinner_item)
        typeSpinner.adapter = adapter

        val isFromProfile = intent.getBooleanExtra("fromProfile", false)
        skipButton.visibility = if (isFromProfile) View.GONE else View.VISIBLE

        addButton.setOnClickListener {
            val dbHelper = moneySplit_Database(this)
            val selectedType = typeSpinner.selectedItem.toString()
            val last4 = last4Input.text.toString().trim()

            if (selectedType == "Select Type") {
                Toast.makeText(this, "Please select a type.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (last4.isEmpty() || last4.length != 4) {
                Toast.makeText(this, "Please enter exactly 4 digits.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val addedDatetime = System.currentTimeMillis().toString()
            val success = dbHelper.insertCard(username!!, selectedType, last4, addedDatetime)

            if (success) {
                if (!isFromProfile) {
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                }

                val intent = Intent(this, Home::class.java).apply {
                    putExtra("username", username)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to add card", Toast.LENGTH_SHORT).show()
            }
        }

        val infoClickListener = {
            AlertDialog.Builder(this)
                .setTitle("Why We Ask This")
                .setMessage(
                    "We use the last 4 digits of your card or account number to accurately detect your transaction SMS and filter out unrelated messages. This makes tracking more reliable. We never ask for your full card number. \n" +
                            "Note: You can add this later in settings."
                )
                .setPositiveButton("Got it", null)
                .show()
        }

        whyIcon.setOnClickListener { infoClickListener() }
        whyText.setOnClickListener { infoClickListener() }

        skipButton.setOnClickListener {
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, Home::class.java).apply {
                putExtra("username", username)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
