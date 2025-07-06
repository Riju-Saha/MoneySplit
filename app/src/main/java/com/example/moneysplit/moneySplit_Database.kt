package com.example.moneysplit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class moneySplit_Database(ctx: Context) :
    SQLiteOpenHelper(ctx, "moneySplit_db.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                username TEXT UNIQUE,
                password TEXT,
                logged_in INTEGER DEFAULT 0
            );
        """)

        db.execSQL("""
        CREATE TABLE IF NOT EXISTS cards (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT,
            card_type TEXT,
            card_last4 TEXT,
            added_datetime TEXT
        );
    """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS plan_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                plan_name TEXT UNIQUE,
                num_participants INTEGER,
                participants TEXT,
                created_on TEXT,
                created_at TEXT,
                created_by TEXT
            );
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS expense_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                amount REAL,
                purpose TEXT,
                type TEXT NOT NULL,
                date_time TEXT NOT NULL,
                mode_of_payment TEXT,
                created_on TEXT DEFAULT (datetime('now', 'localtime'))
            );
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS plan_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                plan_name TEXT,
                amount REAL,
                purpose TEXT,
                date TEXT,
                paid_by TEXT
            );
        """)

        db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_splits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expense_id INTEGER,
                    participant TEXT,
                    amount REAL
            );
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS plan_table")
        db.execSQL("DROP TABLE IF EXISTS expense_table")
        db.execSQL("DROP TABLE IF EXISTS plan_expenses")
        db.execSQL("DROP TABLE IF EXISTS custom_splits")
        db.execSQL("DROP TABLE IF EXISTS cards")
        onCreate(db)
    }

    data class User(
        val name: String,
        val username: String,
        val password: String
    )

    data class Card(
        val id: Int,
        val username: String,
        val cardType: String,
        val cardLast4: String,
        val addedDateTime: String
    )


    data class Plan(
        val name: String,
        val participants: Int
    )

    data class Expense(
        val id: Int,
        val amount: Double,
        val purpose: String,
        val type: String,
        val dateTime: String,
        val modeOfPayment: String
    )

    data class PlanExpense(
        val id: Int,
        val amount: Double,
        val purpose: String,
        val date: String,
        val paidBy: String
    )

    data class CustomSplit(
        val participant: String,
        val amount: Double
    )

    fun insertUser(name: String, username: String, password: String): Boolean {
        if (isUsernameExists(username)) return false
        val db = writableDatabase
        db.execSQL("UPDATE users SET logged_in = 0")
        val values = ContentValues().apply {
            put("name", name)
            put("username", username)
            put("password", password)
        }
        val result = db.insert("users", null, values)
        db.close()
        return result != -1L
    }

    fun isUsernameExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", arrayOf(username))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun checkUser(username: String, password: String): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", arrayOf(username, password))
        val success = cursor.moveToFirst()
        cursor.close()

        if (success) {
            db.execSQL("UPDATE users SET logged_in = 0")
            db.execSQL("UPDATE users SET logged_in = 1 WHERE username = ?", arrayOf(username))
        }

        db.close()
        return success
    }

    fun getUserByUsername(username: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name, username, password FROM users WHERE username = ?", arrayOf(username))
        return if (cursor.moveToFirst()) {
            val user = User(
                name = cursor.getString(0),
                username = cursor.getString(1),
                password = cursor.getString(2)
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun updateUser(oldUsername: String, newName: String, newUsername: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", newName)
            put("username", newUsername)
            put("password", newPassword)
        }
        val result = db.update("users", values, "username = ?", arrayOf(oldUsername))
        return result > 0
    }

    fun insertCard(username: String, cardType: String, last4: String, addedDatetime: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("card_type", cardType)
            put("card_last4", last4)
            put("added_datetime", addedDatetime)
        }
        val result = db.insert("cards", null, values)
        return result != -1L
    }

    fun getCardsForUser(username: String): List<Card> {
        val cards = mutableListOf<Card>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM cards WHERE username = ?", arrayOf(username))
        if (cursor.moveToFirst()) {
            do {
                val card = Card(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cardType = cursor.getString(cursor.getColumnIndexOrThrow("card_type")),
                    cardLast4 = cursor.getString(cursor.getColumnIndexOrThrow("card_last4")),
                    addedDateTime = cursor.getString(cursor.getColumnIndexOrThrow("added_datetime"))
                )
                cards.add(card)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return cards
    }

    fun getCardLast4Digits(username: String): List<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT card_Last4 FROM cards WHERE username = ?", arrayOf(username))
        val list = mutableListOf<String>()
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list
    }


    fun deleteCard(cardId: Int, username: String) {
        val db = writableDatabase
        db.delete("cards", "id = ? AND username = ?", arrayOf(cardId.toString(), username))
    }

    fun getTotalForMonthByTypeFromExpenseUsingLike(month: String, year: String, type: String, username: String): Double {
//        val username = getLoggedInUsername() ?: return 0.0
        val db = readableDatabase
        val yearMonthPattern = "$year-$month%"  // e.g., 2025-06%
        val query = "SELECT SUM(amount) FROM expense_table WHERE date_time LIKE ? AND type = ? COLLATE NOCASE AND username = ?"
        Log.d("DB_QUERY", "Running LIKE query: $query with values:  $yearMonthPattern, type=$type")

        val cursor = db.rawQuery(query, arrayOf(yearMonthPattern, type, username))
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
            Log.d("DB_RESULT", "Fetched total: $total for LIKE pattern=$yearMonthPattern and type=$type")
        } else {
            Log.d("DB_RESULT", "No matching data for LIKE pattern=$yearMonthPattern and type=$type")
        }
        cursor.close()
        return total
    }

    fun isIternaryExists(planName: String, createdBy: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM plan_table WHERE plan_name = ? AND created_by = ?",
            arrayOf(planName.trim(), createdBy.trim())
        )
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun insertIternary(planName: String, numParticipants: Int, participants: String, createdOn: String, createdAt: String, createdBy: String): Boolean {
//        val createdBy = getLoggedInUsername() ?: return false
        val db = writableDatabase
        val values = ContentValues().apply {
            put("plan_name", planName)
            put("num_participants", numParticipants)
            put("participants", participants)
            put("created_on", createdOn)
            put("created_at", createdAt)
            put("created_by", createdBy)
        }
        val result = db.insert("plan_table", null, values)
        db.close()
        return result != -1L
    }

    fun getAllIternaries(username: String): List<Plan> {
        val db = readableDatabase
        val list = mutableListOf<Plan>()
        val cursor = db.rawQuery(
            "SELECT plan_name, num_participants FROM plan_table WHERE created_by = ?",
            arrayOf(username)
        )
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(0)
                val count = cursor.getInt(1)
                list.add(Plan(name, count))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun getAllPlanNames(username: String): List<String> {
        val planNames = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT plan_name FROM plan_table WHERE participants LIKE ? AND created_by = ?",
            arrayOf("%${username?.trim()}%", username?.trim())
        )

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("plan_name"))
                planNames.add(name)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return planNames
    }

    fun isExpenseAlreadyExists(username: String, amount: Double, purpose: String, type: String, dateTime: String, modeOfPayment: String): Boolean {
        val db = this.readableDatabase
        val query = """
        SELECT 1 FROM expense_table
        WHERE username = ? AND amount = ? AND purpose = ? AND type = ? AND date_time = ? AND mode_of_payment = ?
        LIMIT 1
    """
        val cursor = db.rawQuery(query, arrayOf(username, amount.toString(), purpose, type, dateTime, modeOfPayment))
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }


    fun insertExpenseWithTimeAndMode(username: String, amount: Double, purpose: String, type: String, dateTime: String, modeOfPayment: String): Boolean {
        if (isExpenseAlreadyExists(username, amount, purpose, type, dateTime, modeOfPayment)) {
            Log.d("DB", "Duplicate expense detected. Skipping insert.")
            return false
        }

        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("amount", amount)
            put("purpose", purpose)
            put("type", type)
            put("date_time", dateTime)
            put("mode_of_payment", modeOfPayment)
        }

        val result = db.insert("expense_table", null, values)
        db.close()
        return result != -1L
    }


    fun getAllExpenses(username: String): MutableList<Expense> {
        val expenses = mutableListOf<Expense>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM expense_table WHERE username = ?",
            arrayOf(username)
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))
                val purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"))
                val modeOfPayment = cursor.getString(cursor.getColumnIndexOrThrow("mode_of_payment"))

                val expense = Expense(id, amount, purpose, type, dateTime, modeOfPayment)
                expenses.add(expense)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return expenses
    }


    fun updateExpense(
        username: String,
        expense: Expense,
        newAmount: Double,
        newPurpose: String,
        newDateTime: String,
        newModeOfPayment: String  // ✅ new argument
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", newAmount)
            put("purpose", newPurpose)
            put("date_time", newDateTime)
            put("mode_of_payment", newModeOfPayment) // ✅ update this too
        }

        val result = db.update(
            "expense_table",
            values,
            "username = ? AND amount = ? AND purpose = ? AND type = ? AND date_time = ? AND mode_of_payment = ?",
            arrayOf(
                username,
                expense.amount.toString(),
                expense.purpose,
                expense.type,
                expense.dateTime,
                expense.modeOfPayment // ✅ match old mode too
            )
        )

        db.close()
        return result > 0
    }


    fun deleteExpense(username: String, expense: Expense): Boolean {
        val db = writableDatabase
        val result = db.delete(
            "expense_table",
            "amount = ? AND purpose = ? AND type = ? AND date_time = ? AND username = ?",
            arrayOf(
                expense.amount.toString(),
                expense.purpose,
                expense.type,
                expense.dateTime,
                username
            )
        )
        db.close()
        return result > 0
    }

    fun getParticipantsForPlan(planName: String): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT participants FROM plan_table WHERE plan_name = ?", arrayOf(planName))
        return if (cursor.moveToFirst()) {
            cursor.getString(0).split(",").map { it.trim() }
        } else {
            cursor.close()
            emptyList<String>()
        }
    }

    fun insertPlanExpense(plan_name: String, amount: Double, purpose: String, date: String, paidBy: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("plan_name", plan_name)
            put("amount", amount)
            put("purpose", purpose)
            put("date", date)
            put("paid_by", paidBy)
        }
        val id = db.insert("plan_expenses", null, values)
        db.close()
        return id.toInt()
    }

    fun getExpensesForPlan(plan: String): List<PlanExpense> {
        val list = mutableListOf<PlanExpense>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, amount, purpose, date, paid_by FROM plan_expenses WHERE plan_name = ?",
            arrayOf(plan)
        )
        while (cursor.moveToNext()) {
            list.add(
                PlanExpense(
                    id = cursor.getInt(0),
                    amount = cursor.getDouble(1),
                    purpose = cursor.getString(2),
                    date = cursor.getString(3),
                    paidBy = cursor.getString(4)
                )
            )
        }
        cursor.close()
        db.close()
        return list
    }

    fun insertCustomSplit(expenseId: Int, participant: String, amount: Double): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("expense_id", expenseId)
            put("participant", participant)
            put("amount", amount)
        }
        val result = db.insert("custom_splits", null, values)
        db.close()
        return result != -1L
    }

    fun deletePlanExpense(expenseId: Int) {
        val db = writableDatabase
        db.delete("plan_expenses", "id = ?", arrayOf(expenseId.toString()))
        db.close()
    }

    fun deleteCustomSplitsForExpense(expenseId: Int) {
        val db = writableDatabase
        db.delete("custom_splits", "expense_id = ?", arrayOf(expenseId.toString()))
        db.close()
    }

    fun getCustomSplitForExpense(expenseId: Int): List<CustomSplit> {
        val list = mutableListOf<CustomSplit>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT participant, amount FROM custom_splits WHERE expense_id = ?",
            arrayOf(expenseId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(0)
                val amount = cursor.getDouble(1)
                list.add(CustomSplit(name, amount))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun updatePlanExpense(expenseId: Int, amount: Double, purpose: String, date: String, paidBy: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", amount)
            put("purpose", purpose)
            put("date", date)
            put("paid_by", paidBy)
        }
        val rows = db.update("plan_expenses", values, "id = ?", arrayOf(expenseId.toString()))
        db.close()
        return rows > 0
    }

}