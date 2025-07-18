package com.example.moneysplit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlansFragment : Fragment() {

    private lateinit var dbHelper: FirebaseHelper
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.plans_fragment, container, false)

        listView = view.findViewById(R.id.plansListView)
        dbHelper = FirebaseHelper()

        loadPlans()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        val username = SessionManager.getLoggedInUsername(requireContext())?.trim()
        Log.d("PlansFragment", "Logged in username: $username")

        if (username == null) {
            Log.e("PlansFragment", "Username is null")
            return
        }

        dbHelper.getAllPlan(username) { plansList ->
            Log.d("PlansFragment", "Plans found: ${plansList.size}")

            if (plansList.isNotEmpty()) {
                val adapter = object : ArrayAdapter<Plan>(
                    requireContext(),
                    R.layout.item_plan_row,
                    R.id.planNameTextView,
                    plansList
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val inflater = LayoutInflater.from(context)
                        val view = convertView ?: inflater.inflate(R.layout.item_plan_row, parent, false)

                        val planNameView = view.findViewById<TextView>(R.id.planNameTextView)
                        val participantCountView = view.findViewById<TextView>(R.id.participantCountTextView)

                        val plan = getItem(position)
                        planNameView.text = plan?.planName
                        participantCountView.text = "👥 ${plan?.numParticipants}"

                        return view
                    }
                }

                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val selectedPlan = plansList[position].planName
                    val intent = Intent(requireContext(), PlanDetailsActivity::class.java)
                    intent.putExtra("planName", selectedPlan)
                    startActivity(intent)
                }
            } else {
                listView.adapter = null
                Log.d("PlansFragment", "No plans found.")
            }
        }
    }
}
