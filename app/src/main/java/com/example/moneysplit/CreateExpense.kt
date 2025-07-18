package com.example.moneysplit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CreateExpense : AppCompatActivity() {

    private lateinit var amountInput: EditText
    private lateinit var purposeInput: EditText
    private lateinit var creditRadio: RadioButton
    private lateinit var debitRadio: RadioButton
    private lateinit var paymentModeSpinner: Spinner
    private lateinit var addButton: Button
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_expense)
        supportActionBar?.hide()

        amountInput = findViewById(R.id.amountInput)
        purposeInput = findViewById(R.id.purposeInput)
        creditRadio = findViewById(R.id.radioCredit)
        debitRadio = findViewById(R.id.radioDebit)
        paymentModeSpinner = findViewById(R.id.paymentModeSpinner)
        addButton = findViewById(R.id.addButton)
        dateTextView = findViewById(R.id.dateTextView)
        timeTextView = findViewById(R.id.timeTextView)

        val username = intent.getStringExtra("username") ?: "User"

        // Set default date and time
        updateDateText()
        updateTimeText()

        dateTextView.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
                updateDateText()
            }, year, month, day)

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        timeTextView.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, m ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                selectedDate.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                selectedDate.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                selectedDate.set(Calendar.HOUR_OF_DAY, h)
                selectedDate.set(Calendar.MINUTE, m)
                selectedDate.set(Calendar.SECOND, 0)

                val now = Calendar.getInstance()

                if (selectedDate.after(now)) {
                    Toast.makeText(this, "Future time not allowed", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, m)
                updateTimeText()
            }, hour, minute, true).show()
        }

        val db = FirebaseHelper()

        db.getCardsForUser(username) { cardsList ->
            val paymentModes = cardsList.map { "${it.cardType} - ${it.cardLast4}" }.toMutableList()
            paymentModes.add("Cash")

            val adapter = ArrayAdapter(this, R.layout.spinner_item, paymentModes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paymentModeSpinner.adapter = adapter
        }

        addButton.setOnClickListener {
            val selectedMode = paymentModeSpinner.selectedItem?.toString() ?: "Cash"
            val amount = amountInput.text.toString().toDoubleOrNull()
            val purpose = purposeInput.text.toString().trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val type = if (creditRadio.isChecked) "Credit" else "Debit"

            val selectedTime = calendar.time
            val currentTime = Date()

            if (amount == null || purpose.isBlank()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTime.after(currentTime)) {
                Toast.makeText(this, "Future date-time not allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedTime)

            db.insertExpenseWithTimeAndMode(
                username,
                amount,
                purpose,
                type,
                formattedDateTime,
                selectedMode
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Home::class.java)
                    intent.putExtra("username", username)
                    intent.putExtra("openFragment", "expense")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateTextView.text = dateFormat.format(calendar.time)
    }

    private fun updateTimeText() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour format
        timeTextView.text = timeFormat.format(calendar.time)
    }
}
