package com.example.moneysplit

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class PlanDetailsActivity : AppCompatActivity() {

    private lateinit var dbHelper: FirebaseHelper
    private lateinit var planTitle: TextView
    private lateinit var inputAmount: EditText
    private lateinit var inputPurpose: EditText
    private lateinit var dateInput: EditText
    private lateinit var dateIcon: ImageView
    private lateinit var paidBySpinner: Spinner
    private lateinit var splitTypeSpinner: Spinner
    private lateinit var addExpenseButton: Button
    private lateinit var expensesTable: TableLayout

    private var editingExpenseId: String? = null  // ✅ Should be String, since Firebase keys are Strings

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_details)
        supportActionBar?.hide()

        dbHelper = FirebaseHelper()

        planTitle = findViewById(R.id.planTitle)
        inputAmount = findViewById(R.id.inputAmount)
        inputPurpose = findViewById(R.id.inputPurpose)
        dateInput = findViewById(R.id.dateInput)
        dateIcon = findViewById(R.id.dateIcon)
        paidBySpinner = findViewById(R.id.paidBySpinner)
        splitTypeSpinner = findViewById(R.id.splitTypeSpinner)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        expensesTable = findViewById(R.id.expensesTable)

        val planName = intent.getStringExtra("planName") ?: "Unknown"
        planTitle.text = planName

        // Load participants for spinner
        dbHelper.getParticipantsForPlan(planName) { participants ->
            Log.d("PlanDetailsActivity", "Participants: $participants")
            val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_item, participants)
            spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
            paidBySpinner.adapter = spinnerAdapter

            intent.getStringExtra("prefillPaidBy")?.let { prefillUser ->
                val index = participants.indexOf(prefillUser)
                if (index >= 0) {
                    paidBySpinner.setSelection(index)
                    paidBySpinner.isEnabled = false
                }
            }
        }


        // Prefill other fields
        intent.getDoubleExtra("prefillAmount", -1.0).takeIf { it != -1.0 }?.let {
            inputAmount.setText(it.toString())
            inputAmount.isEnabled = false
        }
        intent.getStringExtra("prefillPurpose")?.let {
            inputPurpose.setText(it)
            inputPurpose.isEnabled = false
        }
        intent.getStringExtra("prefillDate")?.let {
            dateInput.setText(it)
            dateInput.isEnabled = false
            dateIcon.isEnabled = false
        }

        val splitAdapter = ArrayAdapter(this, R.drawable.spinner_item, listOf("Equally", "Custom"))
        splitAdapter.setDropDownViewResource(R.drawable.spinner_item)
        splitTypeSpinner.adapter = splitAdapter

        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(year, month, day)
            dateInput.setText(dateFormatter.format(calendar.time))
        }

        dateInput.setOnClickListener {
            DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.maxDate = System.currentTimeMillis() }
                .show()
        }

        dateIcon.setOnClickListener {
            DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.maxDate = System.currentTimeMillis() }
                .show()
        }

        addExpenseButton.setOnClickListener {
            val amount = inputAmount.text.toString().toDoubleOrNull()
            val purpose = inputPurpose.text.toString()
            val date = dateInput.text.toString()
            val paidBy = paidBySpinner.selectedItem?.toString() ?: ""
            val splitType = splitTypeSpinner.selectedItem?.toString()

            if (amount == null || purpose.isBlank() || date.isBlank() || paidBy.isBlank() || splitType == null) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.getParticipantsForPlan(planName) { participants ->
                if (participants.isEmpty()) {
                    Toast.makeText(this, "No participants found!", Toast.LENGTH_SHORT).show()
                    return@getParticipantsForPlan
                }

                if (editingExpenseId != null) {
                    // ✅ Firebase update is async
                    dbHelper.updatePlanExpense(planName, editingExpenseId!!, amount, purpose, date, paidBy) { success ->
                        if (!success) {
                            Toast.makeText(this, "Failed to update expense", Toast.LENGTH_SHORT).show()
                            return@updatePlanExpense
                        }

                        handleSplits(splitType, participants, amount, planName, editingExpenseId!!)
                    }
                } else {
                    // ✅ Insert new expense async
                    dbHelper.insertPlanExpense(planName, amount, purpose, date, paidBy) { newId ->
                        if (newId == null) {
                            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
                            return@insertPlanExpense
                        }

                        handleSplits(splitType, participants, amount, planName, newId)
                    }
                }
            }
        }

        loadExpenses(planName)
    }

    private fun handleSplits(splitType: String, participants: List<String>, amount: Double, planName: String, expenseId: String) {
        dbHelper.deleteCustomSplitsForExpense(planName, expenseId) { _ ->

            if (splitType == "Equally") {
                val share = String.format("%.2f", amount / participants.size).toDouble()
                for (p in participants) {
                    dbHelper.insertCustomSplit(planName, expenseId, p, share) {}
                }
                Toast.makeText(this, "Expense saved and split equally", Toast.LENGTH_SHORT).show()
                resetForm()
                loadExpenses(planName)
            } else {
                showCustomSplitDialog(participants, amount, expenseId, planName)
            }
        }
    }

    private fun loadExpenses(planName: String) {
        expensesTable.removeAllViews()

        val headerRow = TableRow(this)
        val headers = listOf("Amount", "Purpose", "Date", "Paid By", "Split")
        headers.forEach {
            val textView = TextView(this).apply {
                text = it
                setPadding(8, 8, 8, 8)
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            headerRow.addView(textView)
        }
        expensesTable.addView(headerRow)

        dbHelper.getPlanExpense(planName) { expenses ->
            val sorted = expenses.sortedByDescending { it.date }
            for (expense in sorted) {
                val row = TableRow(this)
                dbHelper.getCustomSplitForExpense(planName, expense.id) { splits ->
                    val splitText = splits.joinToString("\n") { "${it.participant}: ₹${String.format("%.2f", it.amount)}" }

                    listOf(
                        "₹${expense.amount}",
                        expense.purpose,
                        expense.date,
                        expense.paidBy,
                        splitText
                    ).forEach {
                        val tv = TextView(this).apply {
                            text = it
                            setPadding(8, 8, 8, 8)
                            setTextColor(android.graphics.Color.WHITE)
                        }
                        row.addView(tv)
                    }

                    row.setOnLongClickListener {
                        dbHelper.getParticipantsForPlan(planName) { participants ->
                            AlertDialog.Builder(this)
                                .setTitle("Select Action")
                                .setItems(arrayOf("Edit", "Delete")) { _, which ->
                                    when (which) {
                                        0 -> {
                                            editingExpenseId = expense.id
                                            inputAmount.setText(expense.amount.toString())
                                            inputPurpose.setText(expense.purpose)
                                            dateInput.setText(expense.date)
                                            val index = participants.indexOf(expense.paidBy)
                                            if (index >= 0) paidBySpinner.setSelection(index)
                                        }
                                        1 -> {
                                            dbHelper.deletePlanExpense(planName, expense.id) {}
                                            dbHelper.deleteCustomSplitsForExpense(planName, expense.id) {}
                                            loadExpenses(planName)
                                            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .show()
                        }
                        true
                    }

                    expensesTable.addView(row)
                }
            }
        }
    }

    private fun showCustomSplitDialog(participants: List<String>, totalAmount: Double, expenseId: String, planName: String) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_custom_split, null)
        val container = view.findViewById<LinearLayout>(R.id.customSplitContainer)
        val amountInputs = mutableMapOf<String, EditText>()
        val remainingText = view.findViewById<TextView>(R.id.remainingAmountText)

        fun updateRemaining() {
            var enteredTotal = 0.0
            for (input in amountInputs.values) {
                enteredTotal += input.text.toString().toDoubleOrNull() ?: 0.0
            }
            val remaining = totalAmount - enteredTotal
            val formatted = String.format("%.2f", remaining)
            remainingText.text = "Remaining to split: ₹$formatted"

            remainingText.setTextColor(
                when {
                    remaining == 0.0 -> android.graphics.Color.GREEN
                    remaining < 0.0 -> android.graphics.Color.RED
                    else -> android.graphics.Color.YELLOW
                }
            )
        }

        for (p in participants) {
            val label = TextView(this)
            label.text = "$p: "
            label.setTextColor(android.graphics.Color.WHITE)

            val input = EditText(this).apply {
                hint = "Amount"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) = updateRemaining()
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }

            container.addView(label)
            container.addView(input)
            amountInputs[p] = input
        }

        updateRemaining()

        val dialogBuilder = AlertDialog.Builder(this)
        val dialog = dialogBuilder.setTitle("Custom Split")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                dbHelper.deletePlanExpense(planName, expenseId) {}
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.isEnabled = false

            saveButton.setOnClickListener {
                for ((name, input) in amountInputs) {
                    val amt = input.text.toString().toDoubleOrNull() ?: 0.0
                    dbHelper.insertCustomSplit(planName, expenseId, name, amt) {}
                }
                Toast.makeText(this, "Custom split saved", Toast.LENGTH_SHORT).show()
                resetForm()
                loadExpenses(planName)
                dialog.dismiss()
            }

            fun validateSplitSum() {
                var total = 0.0
                for ((_, input) in amountInputs) {
                    total += input.text.toString().toDoubleOrNull() ?: 0.0
                }
                val expected = String.format("%.2f", totalAmount).toDouble()
                val entered = String.format("%.2f", total).toDouble()
                saveButton.isEnabled = entered == expected
                updateRemaining()
            }

            for (input in amountInputs.values) {
                input.addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) = validateSplitSum()
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
        }

        dialog.show()
    }

    private fun resetForm() {
        inputAmount.text.clear()
        inputPurpose.text.clear()
        dateInput.text.clear()
        editingExpenseId = null
    }
}
