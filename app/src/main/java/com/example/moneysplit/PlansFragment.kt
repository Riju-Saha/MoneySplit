package com.example.moneysplit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class PlansFragment : Fragment() {

    private lateinit var dbHelper: moneySplit_Database
    private lateinit var listView: ListView
    private var plans: List<moneySplit_Database.Plan> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
//        val username = SessionManager.getLoggedInUsername(requireContext())
    ): View? {
        val view = inflater.inflate(R.layout.plans_fragment, container, false)

        listView = view.findViewById(R.id.plansListView)
        dbHelper = moneySplit_Database(requireContext())

        loadPlans()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        val username = SessionManager.getLoggedInUsername(requireContext())
        plans = dbHelper.getAllIternaries(username.toString())

        if (plans.isNotEmpty()) {
            val adapter = object : ArrayAdapter<moneySplit_Database.Plan>(
                requireContext(),
                R.layout.item_plan_row,
                R.id.planNameTextView,
                plans
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val inflater = LayoutInflater.from(context)
                    val view = convertView ?: inflater.inflate(R.layout.item_plan_row, parent, false)

                    val planNameView = view.findViewById<TextView>(R.id.planNameTextView)
                    val participantCountView = view.findViewById<TextView>(R.id.participantCountTextView)

                    val plan = getItem(position)
                    planNameView.text = plan?.name
                    participantCountView.text = "👥 ${plan?.participants}"

                    return view
                }
            }

            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedPlan = plans[position].name
                val intent = Intent(requireContext(), PlanDetailsActivity::class.java)
                intent.putExtra("planName", selectedPlan)
                startActivity(intent)
            }
        } else {
            listView.adapter = null
        }
    }
}


