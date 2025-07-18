package com.example.moneysplit

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

data class User(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val logged_in: Int = 0
)


data class Card(
    val id: String? = null,
    val username: String = "",
    val cardType: String = "",
    val cardLast4: String = "",
    val addedDateTime: String = ""
)

data class Expense(
    val amount: Double = 0.0,
    val purpose: String = "",
    val type: String = "",
    val dateTime: String = "",
    val modeOfPayment: String = ""
)

data class Plan(
    val planName: String = "",
    val numParticipants: Int = 0,
    val participants: List<String> = listOf(),
    val createdOn: String = "",
    val createdAt: String = "",
    val createdBy: String = ""
)

data class PlanExpense(
    val id: String = "",      // Firebase push key
    val amount: Double = 0.0,
    val purpose: String = "",
    val date: String = "",
    val paidBy: String = ""
)

data class CustomSplit(
    val participant: String = "",
    val amount: Double = 0.0
)

class FirebaseHelper {
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")
    private val cardsRef = FirebaseDatabase.getInstance().getReference("cards")
    private val expense_table_Ref = FirebaseDatabase.getInstance().getReference("expense_table")
    private val plan_table_Ref = FirebaseDatabase.getInstance().getReference("plan_table")
    private val plan_expenses_Ref = FirebaseDatabase.getInstance().getReference("plan_expenses")
    private val custom_splits_Ref = FirebaseDatabase.getInstance().getReference("custom_splits")

    fun isUsernameExists(username: String, onResult: (Boolean) -> Unit) {
        usersRef.orderByChild("username").equalTo(username).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // Insert user (after checking if username exists)
    fun insertUser(name: String, username: String, password: String, onResult: (Boolean) -> Unit) {
        isUsernameExists(username) { exists ->
            if (exists) {
                onResult(false)  // Username taken
            } else {
                // Set all users logged_in = 0 first
                usersRef.get().addOnSuccessListener { snapshot ->
                    snapshot.children.forEach { userSnap ->
                        userSnap.ref.child("logged_in").setValue(0)
                    }

                    // Add new user with logged_in = 1
                    val newUser = User(name, username, password)
                    val key = usersRef.push().key
                    if (key != null) {
                        usersRef.child(key).setValue(newUser)
                            .addOnSuccessListener { onResult(true) }
                            .addOnFailureListener { onResult(false) }
                    } else {
                        onResult(false)
                    }
                }.addOnFailureListener {
                    onResult(false)
                }
            }
        }
    }

    fun checkUser(username: String, password: String, onResult: (Boolean) -> Unit) {
        Log.d("LoginDebug", "Checking user: $username with password: $password")

        usersRef.orderByChild("username").equalTo(username).get()
            .addOnSuccessListener { snapshot ->
                Log.d("LoginDebug", "Snapshot result: ${snapshot.value}")

                if (snapshot.exists()) {
                    val userSnap = snapshot.children.first()
                    val user = userSnap.getValue(User::class.java)
                    Log.d("LoginDebug", "Fetched user: $user")

                    if (user != null && user.password == password) {
                        // Optional: mark as logged_in = 1
                        userSnap.ref.child("logged_in").setValue(1)
                        onResult(true)
                    } else {
                        Log.d("LoginDebug", "Password did not match")
                        onResult(false)
                    }
                } else {
                    Log.d("LoginDebug", "No user found for username: $username")
                    onResult(false)
                }
            }
            .addOnFailureListener {
                Log.e("LoginDebug", "Firebase failed: ${it.message}")
                onResult(false)
            }
    }



    // ✅ Get user by username
    fun getUserByUsername(username: String, onResult: (User?) -> Unit) {
        usersRef.orderByChild("username").equalTo(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.children.first().getValue(User::class.java)
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // ✅ Update user details
    fun updateUser(oldUsername: String, newName: String, newUsername: String, newPassword: String, onResult: (Boolean) -> Unit) {
        usersRef.orderByChild("username").equalTo(oldUsername).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val key = snapshot.children.first().key
                    if (key != null) {
                        val updates = mapOf(
                            "name" to newName,
                            "username" to newUsername,
                            "password" to newPassword
                        )
                        usersRef.child(key).updateChildren(updates).addOnCompleteListener { task ->
                            onResult(task.isSuccessful)
                        }
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun insertCard(username: String, cardType: String, last4: String, addedDatetime: String, onComplete: (Boolean) -> Unit) {
        val card = Card(
            id = null, // Firebase will generate ID
            username = username,
            cardType = cardType,
            cardLast4 = last4,
            addedDateTime = addedDatetime
        )

        val pushRef = cardsRef.child(username).push()
        pushRef.setValue(card)
            .addOnSuccessListener {
                println("DEBUG: Card inserted with ID = ${pushRef.key}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                println("DEBUG: Failed to insert card: ${e.message}")
                onComplete(false)
            }
    }


    // ✅ Get all cards for a user
    fun getCardsForUser(username: String, onResult: (List<Card>) -> Unit) {
        cardsRef.child(username).get()
            .addOnSuccessListener { snapshot ->
                val cards = mutableListOf<Card>()
                snapshot.children.forEach { child ->
                    val card = child.getValue(Card::class.java)
                    val id = child.key // ✅ Firebase push ID
                    if (card != null && id != null) {
                        cards.add(card.copy(id = id))
                    }
                }
                onResult(cards)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }


    // ✅ Get DISTINCT last 4 digits for user
    fun getCardLast4Digits(username: String, onResult: (List<String>) -> Unit) {
        cardsRef.orderByChild("username").equalTo(username).get()
            .addOnSuccessListener { snapshot ->
                val last4Set = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    val card = child.getValue(Card::class.java)
                    if (card != null) {
                        last4Set.add(card.cardLast4)
                    }
                }
                onResult(last4Set.toList())
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // ✅ Delete card by Firebase key
    fun deleteCard(username: String, cardId: String, onResult: (Boolean) -> Unit) {
        cardsRef.child(username).child(cardId).removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun isExpenseAlreadyExists(username: String, amount: Double, purpose: String, type: String, dateTime: String, modeOfPayment: String, onResult: (Boolean) -> Unit) {
        expense_table_Ref.child(username)
            .orderByChild("dateTime").equalTo(dateTime)
            .get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.children.any { child ->
                    val e = child.getValue(Expense::class.java)
                    e != null && e.amount == amount && e.purpose == purpose && e.type == type && e.modeOfPayment == modeOfPayment
                }
                onResult(exists)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ✅ Insert expense if not duplicate
    fun insertExpenseWithTimeAndMode(username: String, amount: Double, purpose: String, type: String, dateTime: String, modeOfPayment: String, onResult: (Boolean) -> Unit) {
        isExpenseAlreadyExists(username, amount, purpose, type, dateTime, modeOfPayment) { exists ->
            if (exists) {
                onResult(false) // Duplicate, skip insert
            } else {
                val newExpense = Expense(amount, purpose, type, dateTime, modeOfPayment)
                val key = expense_table_Ref.child(username).push().key
                if (key != null) {
                    expense_table_Ref.child(username).child(key).setValue(newExpense)
                        .addOnSuccessListener { onResult(true) }
                        .addOnFailureListener { onResult(false) }
                } else {
                    onResult(false)
                }
            }
        }
    }

    // ✅ Get all expenses for user
    fun getAllExpenses(username: String, onResult: (List<Pair<String, Expense>>) -> Unit) {
        expense_table_Ref.child(username).get()
            .addOnSuccessListener { snapshot ->
                val expenses = mutableListOf<Pair<String, Expense>>()
                snapshot.children.forEach { child ->
                    val expense = child.getValue(Expense::class.java)
                    if (expense != null) {
                        expenses.add(Pair(child.key ?: "", expense))
                    }
                }
                onResult(expenses)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // ✅ Update expense
    fun updateExpense(username: String, oldExpense: Expense, newAmount: Double, newPurpose: String, newDateTime: String, newModeOfPayment: String, onResult: (Boolean) -> Unit) {
        expense_table_Ref.child(username).get()
            .addOnSuccessListener { snapshot ->
                var updated = false
                for (child in snapshot.children) {
                    val expense = child.getValue(Expense::class.java)
                    if (expense != null &&
                        expense.amount == oldExpense.amount &&
                        expense.purpose == oldExpense.purpose &&
                        expense.type == oldExpense.type &&
                        expense.dateTime == oldExpense.dateTime &&
                        expense.modeOfPayment == oldExpense.modeOfPayment
                    ) {

                        val updates = mapOf(
                            "amount" to newAmount,
                            "purpose" to newPurpose,
                            "dateTime" to newDateTime,
                            "modeOfPayment" to newModeOfPayment
                        )
                        child.ref.updateChildren(updates).addOnSuccessListener {
                            onResult(true)
                        }.addOnFailureListener {
                            onResult(false)
                        }
                        updated = true
                        break
                    }
                }
                if (!updated) onResult(false)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ✅ Delete expense
    fun deleteExpense(username: String, expense: Expense, onResult: (Boolean) -> Unit) {
        expense_table_Ref.child(username).get()
            .addOnSuccessListener { snapshot ->
                var deleted = false
                for (child in snapshot.children) {
                    val e = child.getValue(Expense::class.java)
                    if (e != null &&
                        e.amount == expense.amount &&
                        e.purpose == expense.purpose &&
                        e.type == expense.type &&
                        e.dateTime == expense.dateTime &&
                        e.modeOfPayment == expense.modeOfPayment
                    ) {
                        child.ref.removeValue().addOnSuccessListener {
                            onResult(true)
                        }.addOnFailureListener {
                            onResult(false)
                        }
                        deleted = true
                        break
                    }
                }
                if (!deleted) onResult(false)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun getTotalForMonthByTypeFromExpenseUsingLike(month: String, year: String, type: String, username: String, onResult: (Double) -> Unit) {
        val yearMonthPattern = "$year-$month"  // e.g., 2025-06
        expense_table_Ref.child(username).get()
            .addOnSuccessListener { snapshot ->
                var total = 0.0
                snapshot.children.forEach { child ->
                    val expense = child.getValue(Expense::class.java)
                    if (expense != null &&
                        expense.dateTime.startsWith(yearMonthPattern) &&
                        expense.type.equals(type, ignoreCase = true)
                    ) {
                        total += expense.amount
                    }
                }
                onResult(total)
            }
            .addOnFailureListener {
                onResult(0.0)
            }
    }

    fun isPlanExists(planName: String, createdBy: String, onResult: (Boolean) -> Unit) {
        plan_table_Ref.orderByChild("planName").equalTo(planName).get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.children.any { child ->
                    val plan = child.getValue(Plan::class.java)
                    plan?.createdBy == createdBy.trim()
                }
                onResult(exists)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ✅ Insert plan
    fun insertPlan(planName: String, numParticipants: Int, participants: List<String>, createdOn: String, createdAt: String,createdBy: String, onResult: (Boolean) -> Unit) {
        val newPlan = Plan(planName, numParticipants, participants, createdOn, createdAt, createdBy)
        val key = plan_table_Ref.push().key
        if (key != null) {
            plan_table_Ref.child(key).setValue(newPlan)
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } else {
            onResult(false)
        }
    }

    fun getAllPlan(username: String, onResult: (List<Plan>) -> Unit) {
        plan_table_Ref.orderByChild("createdBy").equalTo(username).get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirebaseHelper", "Plans snapshot children: ${snapshot.childrenCount}")
                val plans = mutableListOf<Plan>()
                snapshot.children.forEach { child ->
                    Log.d("FirebaseHelper", "Child key: ${child.key} val: ${child.value}")
                    val plan = child.getValue(Plan::class.java)
                    if (plan != null) plans.add(plan)
                }
                onResult(plans)
            }
            .addOnFailureListener {
                Log.e("FirebaseHelper", "Failed to get plans: ${it.message}")
                onResult(emptyList())
            }
    }
    
    fun getAllPlanNames(username: String, onResult: (List<String>) -> Unit) {
        plan_table_Ref.orderByChild("createdBy").equalTo(username.trim()).get()
            .addOnSuccessListener { snapshot ->
                val planNames = mutableListOf<String>()
                snapshot.children.forEach { child ->
                    val plan = child.getValue(Plan::class.java)
                    if (plan != null && plan.participants.contains(username.trim())) {
                        planNames.add(plan.planName)
                    }
                }
                onResult(planNames)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }


    fun getParticipantsForPlan(planName: String, onResult: (List<String>) -> Unit) {
        plan_table_Ref.orderByChild("planName").equalTo(planName).get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirebaseDebug", "Snapshot exists: ${snapshot.exists()} snapshot: ${snapshot.value}")
                val plan = snapshot.children.firstOrNull()?.getValue(Plan::class.java)
                Log.d("FirebaseDebug", "Plan: $plan")
                if (plan != null && plan.participants.isNotEmpty()) {
                    onResult(plan.participants)
                } else {
                    onResult(emptyList())
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseDebug", "Failed: ${it.message}")
                onResult(emptyList())
            }
    }




    fun insertPlanExpense(planName: String, amount: Double, purpose: String, date: String, paidBy: String, onResult: (String?) -> Unit) {
        val key = plan_expenses_Ref.child(planName).push().key
        if (key != null) {
            val expense = PlanExpense(key, amount, purpose, date, paidBy)
            plan_expenses_Ref.child(planName).child(key).setValue(expense)
                .addOnSuccessListener { onResult(key) }
                .addOnFailureListener { onResult(null) }
        } else {
            onResult(null)
        }
    }

    fun getPlanExpense(planName: String, onResult: (List<PlanExpense>) -> Unit) {
        plan_expenses_Ref.child(planName).get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<PlanExpense>()
                snapshot.children.forEach { child ->
                    val expense = child.getValue(PlanExpense::class.java)
                    if (expense != null) list.add(expense)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun updatePlanExpense(planName: String, expenseId: String, newAmount: Double, newPurpose: String, newDate: String, newPaidBy: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "amount" to newAmount,
            "purpose" to newPurpose,
            "date" to newDate,
            "paidBy" to newPaidBy
        )
        plan_expenses_Ref.child(planName).child(expenseId).updateChildren(updates)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deletePlanExpense(planName: String, expenseId: String, onResult: (Boolean) -> Unit) {
        plan_expenses_Ref.child(planName).child(expenseId).removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun insertCustomSplit(planName: String, expenseId: String, participant: String,amount: Double, onResult: (Boolean) -> Unit) {
        val splitRef = plan_expenses_Ref.child(planName).child(expenseId).child("custom_splits").push()
        val split = CustomSplit(participant, amount)
        splitRef.setValue(split)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteCustomSplitsForExpense(planName: String, expenseId: String, onResult: (Boolean) -> Unit) {
        plan_expenses_Ref.child(planName).child(expenseId).child("custom_splits").removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getCustomSplitForExpense(planName: String, expenseId: String, onResult: (List<CustomSplit>) -> Unit) {
        plan_expenses_Ref.child(planName).child(expenseId).child("custom_splits").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<CustomSplit>()
                snapshot.children.forEach { child ->
                    val split = child.getValue(CustomSplit::class.java)
                    if (split != null) list.add(split)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}