package com.example.moneysplit

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomePagerAdapter(activity: FragmentActivity, private val username: String) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OverviewFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            1 -> ExpensesFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            2 -> PlansFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
