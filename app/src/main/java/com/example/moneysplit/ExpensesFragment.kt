package com.example.moneysplit

import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import android.graphics.Color
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*


class ExpensesFragment : Fragment() {

    private lateinit var dbHelper: FirebaseHelper
    private lateinit var tableLayout: TableLayout
    private var username: String = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username") ?: "User"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.expenses_fragment, container, false)
        dbHelper = FirebaseHelper()
        tableLayout = view.findViewById(R.id.expensesTable)
//        loadExpenses(username)
        return view
    }

    private fun loadExpenses(username: String) {
        Log.d("ExpensesFragment", "Loading expenses for user: $username")
        tableLayout.removeAllViews()

        dbHelper.getAllExpenses(username) { expensesList ->
            Log.d("ExpensesFragment", "Loaded ${expensesList.size} expenses")

            val expenses = expensesList.sortedByDescending { pair -> pair.second.dateTime }

            val headerRow = TableRow(requireContext())
            val headers = listOf("Amount", "Purpose", "Type", "Date", "Time", "Mode")

            headers.forEach {
                val textView = TextView(requireContext()).apply {
                    text = it
                    setTypeface(null, Typeface.BOLD)
                    setPadding(16, 12, 16, 12)
                    setTextColor(Color.WHITE)
                    ellipsize = TextUtils.TruncateAt.END
                    maxLines = 1
                }
                headerRow.addView(textView)
            }
            tableLayout.addView(headerRow)

            for ((id, expense) in expenses) {
                addExpenseRow(expense)
            }
        }
    }


    private fun addExpenseRow(expense: Expense) {
        val dateTimeParts = expense.dateTime.split(" ")
        val rawDate = dateTimeParts.getOrNull(0) ?: ""
        val fullTime = dateTimeParts.getOrNull(1) ?: ""

        val time = fullTime.split(":").let {
            if (it.size >= 2) "${it[0]}:${it[1]}" else ""
        }

        val date = rawDate.split("-").let {
            if (it.size == 3) "${it[2]}-${it[1]}-${it[0]}" else ""
        }

        val row = TableRow(requireContext())

        fun styledText(text: String, color: Int = Color.WHITE): TextView {
            return TextView(requireContext()).apply {
                this.text = text
                setPadding(16, 12, 16, 12)
                setTextColor(color)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
        }

        val amountView = styledText("₹${expense.amount}")
        val purposeView = styledText(expense.purpose)
        val typeView = styledText(
            expense.type,
            if (expense.type == "Credit") 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )
        val dateView = styledText(date)
        val timeView = styledText(time)

        val modeRaw = expense.modeOfPayment.trim()
        val modeMasked = if (modeRaw.length >= 4) modeRaw.takeLast(4) else modeRaw
        val modeView = styledText(modeMasked)

        val moreOption = TextView(requireContext()).apply {
            text = "⋯"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 12, 24, 12)
            setTextColor(Color.WHITE)
            setOnClickListener {
                showPlansDropdown(it, expense)
            }
        }

        row.setOnLongClickListener {
            showEditDeleteDialog(expense)
            true
        }

        row.addView(amountView)
        row.addView(purposeView)
        row.addView(typeView)
        row.addView(dateView)
        row.addView(timeView)
        row.addView(modeView)
        row.addView(moreOption)

        tableLayout.addView(row)
    }

    private fun showEditDeleteDialog(expense: Expense) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(expense)
                    1 -> {
                        dbHelper.deleteExpense(username, expense) { success ->
                            if (success) {
                                Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show()
                                loadExpenses(username)
                            } else {
                                Toast.makeText(requireContext(), "Failed to delete expense", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun showEditDialog(expense: Expense) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_expense, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editAmount)
        val purposeInput = dialogView.findViewById<EditText>(R.id.editPurpose)
        val dateInput = dialogView.findViewById<EditText>(R.id.editDate)
        val timeInput = dialogView.findViewById<EditText>(R.id.editTime)
        val modeInput = dialogView.findViewById<Spinner>(R.id.editModeOfPayment)

        val dateTimeParts = expense.dateTime.split(" ")
        val date = dateTimeParts.getOrNull(0) ?: ""
        val time = dateTimeParts.getOrNull(1) ?: ""

        amountInput.setText(expense.amount.toString())
        purposeInput.setText(expense.purpose)
        dateInput.setText(date)
        timeInput.setText(time)

        val modes = mutableListOf("Cash")
        dbHelper.getCardLast4Digits(username) { savedCards ->
            savedCards.forEach { last4 -> modes.add("Card ($last4)") }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modeInput.adapter = adapter

            val currentMode = expense.modeOfPayment
            val modeIndex = modes.indexOfFirst {
                currentMode.equals(it, ignoreCase = true) || currentMode.contains(it, ignoreCase = true)
            }
            if (modeIndex >= 0) {
                modeInput.setSelection(modeIndex)
            }
        }

        dateInput.setOnClickListener {
            val parts = date.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2024
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1

            val datePicker = DatePickerDialog(requireContext(), { _, y, m, d ->
                val formattedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                dateInput.setText(formattedDate)
            }, year, month, day)

            datePicker.show()
        }

        timeInput.setOnClickListener {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val timePicker = TimePickerDialog(requireContext(), { _, h, m ->
                val formattedTime = "%02d:%02d:00".format(h, m)
                timeInput.setText(formattedTime)
            }, hour, minute, true)

            timePicker.show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newAmount = amountInput.text.toString().toDoubleOrNull()
                val newPurpose = purposeInput.text.toString().trim()
                val newDate = dateInput.text.toString().trim()
                val newTime = timeInput.text.toString().trim()
                val newModeRaw = modeInput.selectedItem?.toString()?.trim() ?: ""
                val newMode = if (newModeRaw.startsWith("Card (") && newModeRaw.endsWith(")")) {
                    newModeRaw.substringAfter("Card (").substringBefore(")").trim()
                } else {
                    newModeRaw
                }

                if (newAmount != null && newPurpose.isNotEmpty() && newDate.isNotEmpty() && newTime.isNotEmpty() && newMode.isNotEmpty()) {
                    val newDateTime = "$newDate $newTime"

                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    try {
                        val selectedDateTime = formatter.parse(newDateTime)
                        val currentDateTime = Date()

                        if (selectedDateTime.after(currentDateTime)) {
                            Toast.makeText(requireContext(), "Cannot select a future date or time", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        dbHelper.updateExpense(username, expense, newAmount, newPurpose, newDateTime, newMode) { success ->
                            if (success) {
                                Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show()
                                loadExpenses(username)
                            } else {
                                Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "Invalid date or time", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlansDropdown(anchor: View, expense: Expense) {
        if (username == "User") {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.getAllPlanNames(username) { plans ->
            if (plans.isEmpty()) {
                Toast.makeText(requireContext(), "No plans found.", Toast.LENGTH_SHORT).show()
                return@getAllPlanNames
            }

            val popupMenu = PopupMenu(requireContext(), anchor)
            plans.forEachIndexed { index, planName ->
                popupMenu.menu.add(Menu.NONE, index, index, planName)
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedPlan = plans[menuItem.itemId]
                val intent = Intent(requireContext(), PlanDetailsActivity::class.java)
                intent.putExtra("planName", selectedPlan)
                intent.putExtra("prefillAmount", expense.amount)
                intent.putExtra("prefillPurpose", expense.purpose)
                intent.putExtra("prefillDate", expense.dateTime.split(" ")[0])
                intent.putExtra("prefillPaidBy", username)
                startActivity(intent)
                true
            }

            popupMenu.show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses(username)
    }
}
