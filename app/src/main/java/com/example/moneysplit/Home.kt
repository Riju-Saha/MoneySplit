package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.core.content.edit
import androidx.core.view.isVisible

class Home : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar?.hide()

        val usernameHead = findViewById<TextView>(R.id.usernameHead)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val viewProfileButton = findViewById<Button>(R.id.viewProfileButton)
        val profileInitial = findViewById<TextView>(R.id.profileInitial)
        val dropdownMenu = findViewById<LinearLayout>(R.id.dropdownMenu)
        val fabMain = findViewById<FloatingActionButton>(R.id.fabMain)
        val fabOption1 = findViewById<FloatingActionButton>(R.id.fabOption1)
        val labelOption1 = findViewById<TextView>(R.id.labelOption1)
        val fabOption2 = findViewById<FloatingActionButton>(R.id.fabOption2)
        val labelOption2 = findViewById<TextView>(R.id.labelOption2)
        val dimBackground = findViewById<View>(R.id.dimBackground)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val username = intent.getStringExtra("username") ?: "User"
        usernameHead.text = "Welcome, $username"

        val openFragment = intent.getStringExtra("openFragment")

        when (openFragment) {
            "plans" -> PlansFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            "expense" -> ExpensesFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            else -> OverviewFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
        }

        val pageIndex = when (openFragment) {
            "plans" -> 2
            "expense" -> 1
            else -> 0
        }
        viewPager.setCurrentItem(pageIndex, false)

        val initial = username.firstOrNull()?.uppercaseChar() ?: 'U'
        profileInitial.text = initial.toString()

        var isFabMenuOpen = false

        fun openFabMenu() {
            fabOption1.show()
            fabOption2.show()
            labelOption1.visibility = View.VISIBLE
            labelOption2.visibility = View.VISIBLE

            // Reset positions
            fabOption1.alpha = 0f
            fabOption2.alpha = 0f
            labelOption1.alpha = 0f
            labelOption2.alpha = 0f

            fabOption1.translationY = 0f
            fabOption2.translationY = 0f
            labelOption1.translationX = -150f  // slide from left
            labelOption2.translationX = -300f

            // Animate FABs upward
            val step = -50f
            fabOption1.animate().translationY(step).alpha(1f).duration = 200
            labelOption1.animate().translationY(step-50f).alpha(1f).duration = 200
            fabOption2.animate().translationY(step-150f).alpha(1f).duration = 200
            labelOption2.animate().translationY(step-200f).alpha(1f).duration = 200

            labelOption1.animate().translationX(0f).alpha(1f).duration = 200
            labelOption2.animate().translationX(0f).alpha(1f).duration = 200

            fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            isFabMenuOpen = true
        }

        fun closeFabMenu() {
            fabOption1.animate().translationY(0f).alpha(0f).setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fabOption1.hide()
                        fabOption1.animate().setListener(null)
                    }
                })

            fabOption2.animate().translationY(0f).alpha(0f).setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fabOption2.hide()
                        fabOption2.animate().setListener(null)
                    }
                })

            labelOption1.animate().translationX(-100f).alpha(0f).setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        labelOption1.visibility = View.GONE
                        labelOption1.animate().setListener(null)
                    }
                })

            labelOption2.animate().translationX(-100f).alpha(0f).setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        labelOption2.visibility = View.GONE
                        labelOption2.animate().setListener(null)
                    }
                })

            fabMain.setImageResource(android.R.drawable.ic_input_add)
            isFabMenuOpen = false
        }


        fun closeAllMenus() {
            closeFabMenu()
            dropdownMenu.visibility = View.GONE
            dimBackground.visibility = View.GONE
        }

        profileInitial.setOnClickListener {
            dropdownMenu.bringToFront()
            if (dropdownMenu.isVisible) {
                dropdownMenu.visibility = View.GONE
                dimBackground.visibility = View.GONE
            } else {
                dropdownMenu.visibility = View.VISIBLE
                dimBackground.visibility = View.VISIBLE
            }
        }

        dimBackground.setOnClickListener {
            closeAllMenus()
        }

        fabMain.setOnClickListener {
            if (isFabMenuOpen) {
                closeAllMenus()
            } else {
                openFabMenu()
                dimBackground.visibility = View.VISIBLE
            }
        }

        fabOption1.setOnClickListener {
            val intent = Intent(this, CreateExpense::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
            closeAllMenus()
        }

        fabOption2.setOnClickListener {
            val intent = Intent(this, CreatePlan::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
            closeAllMenus()
        }

        logoutButton.setOnClickListener {
            SessionManager.logout(this)

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        viewProfileButton.setOnClickListener {
            val intent = Intent(this, ViewProfile::class.java)
            startActivity(intent)
            finish()
        }

        val adapter = HomePagerAdapter(this,username)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Expenses"
                2 -> "Plans"
                else -> "Section ${position + 1}"
            }
        }.attach()

    }
}
