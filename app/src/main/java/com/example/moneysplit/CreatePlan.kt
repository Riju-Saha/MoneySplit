package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CreatePlan : AppCompatActivity() {

    private lateinit var etPeopleCount: EditText
    private lateinit var dynamicNamesContainer: LinearLayout
    private val nameFields = mutableListOf<EditText>()
    private lateinit var dbHelper: moneySplit_Database
    private lateinit var btnSavePlan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_plan)
        supportActionBar?.hide()

        dbHelper = moneySplit_Database(this)

        val etPlanName = findViewById<EditText>(R.id.etPlanName)
        etPeopleCount = findViewById(R.id.etPeopleCount)
        val btnGenerateFields = findViewById<Button>(R.id.btnGenerateFields)
        dynamicNamesContainer = findViewById(R.id.dynamicNamesContainer)
        btnSavePlan = findViewById(R.id.btnSavePlan)

        btnSavePlan.isEnabled = false  // Initially disabled

        val includeSelfCheckbox = findViewById<CheckBox>(R.id.includeSelfCheckbox)
        val selfNameTextView = findViewById<TextView>(R.id.selfNameTextView)
        val username = intent.getStringExtra("username") ?: "User"

        includeSelfCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selfNameTextView.visibility = TextView.VISIBLE
                selfNameTextView.text = username
            } else {
                selfNameTextView.visibility = TextView.GONE
                selfNameTextView.text = ""
            }
        }

        btnGenerateFields.setOnClickListener {
            generateNameFields()
        }

        btnSavePlan.setOnClickListener {
            val planName = etPlanName.text.toString().trim()
            val peopleNames = mutableListOf<String>().apply {
                if (includeSelfCheckbox.isChecked && selfNameTextView.text.isNotEmpty()) {
                    add(selfNameTextView.text.toString().trim())
                }
                addAll(nameFields.map { it.text.toString().trim() })
            }

            if (planName.isEmpty() || peopleNames.any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.isIternaryExists(planName, username)) {
                Toast.makeText(this, "Plan name already exists!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numParticipants = peopleNames.size
            val participants = peopleNames.joinToString(", ")

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val success = dbHelper.insertIternary(planName, numParticipants, participants, date, time, username)

            if (success) {
                Toast.makeText(this, "Plan Saved Successfully!", Toast.LENGTH_SHORT).show()
                etPlanName.text.clear()
                etPeopleCount.text.clear()
                dynamicNamesContainer.removeAllViews()
                nameFields.clear()
                val intent = Intent(this, Home::class.java)
                intent.putExtra("username", username)
                intent.putExtra("openFragment", "plans")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to save plan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateNameFields() {
        val countText = etPeopleCount.text.toString()
        val count = countText.toIntOrNull()

        if (count == null || count <= 0) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        dynamicNamesContainer.removeAllViews()
        nameFields.clear()

        for (i in 1..count) {
            val editText = EditText(this)
            editText.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            editText.hint = "Name $i"
            editText.setHintTextColor(0xFFBBBBBB.toInt())
            editText.setTextColor(0xFFFFFFFF.toInt())
            editText.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            editText.inputType = InputType.TYPE_CLASS_TEXT

            // Add text watcher to each field
            editText.addTextChangedListener(textWatcher)

            dynamicNamesContainer.addView(editText)
            nameFields.add(editText)
        }

        // Re-evaluate button state after generating fields
        evaluateSaveButtonState()
    }

    // TextWatcher to monitor field changes
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            evaluateSaveButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun evaluateSaveButtonState() {
        btnSavePlan.isEnabled = nameFields.isNotEmpty() && nameFields.all { it.text.toString().isNotBlank() }
    }
}
