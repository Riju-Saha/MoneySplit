package com.example.moneysplit

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import android.graphics.Typeface

import java.util.*

class OverviewFragment : Fragment() {

    private lateinit var dbHelper: moneySplit_Database
    private lateinit var totalSpentText: TextView
    private lateinit var creditDebitText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var monthChipGroup: ChipGroup
    private var selectedChip: Chip? = null
    private var username: String = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username") ?: "User"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.overview_fragment, container, false)

        dbHelper = moneySplit_Database(requireContext())
        totalSpentText = view.findViewById(R.id.totalSpentText)
        creditDebitText = view.findViewById(R.id.creditDebitText)
        pieChart = view.findViewById(R.id.pieChart)
        monthChipGroup = view.findViewById(R.id.monthChipGroup)

        setupMonthChips()
        return view
    }

    private fun setupMonthChips() {
        val months = generatePastMonths(6)
        monthChipGroup.removeAllViews()
        months.forEach { addMonthChip(it) }

        if (monthChipGroup.childCount > 0) {
            val firstChip = monthChipGroup.getChildAt(0) as Chip
            firstChip.performClick()
        }
    }

    private fun generatePastMonths(count: Int): List<String> {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM yy", Locale.getDefault())
        val months = mutableListOf<String>()
        repeat(count) {
            months.add(format.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        return months
    }

    private fun addMonthChip(month: String) {
        val chip = Chip(requireContext()).apply {
            text = month
            isCheckable = true
            isClickable = true
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setOnClickListener {
                highlightChip(this)
                loadTotalSpent(month)
            }
        }
        monthChipGroup.addView(chip)
    }

    private fun highlightChip(chip: Chip) {
        selectedChip?.isChecked = false
        chip.isChecked = true
        selectedChip = chip

        chip.parent?.let {
            (it.parent as? HorizontalScrollView)?.post {
                val scrollX = chip.left - 16
                (it.parent as? HorizontalScrollView)?.smoothScrollTo(scrollX, 0)
            }
        }
    }

    private fun loadTotalSpent(monthLabel: String) {
        val inputFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        val parsedDate = inputFormat.parse(monthLabel) ?: return

        val cal = Calendar.getInstance()
        cal.time = parsedDate

        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)  // zero-indexed month
        val year = cal.get(Calendar.YEAR).toString()

        Log.d("OverviewFragment", "Fetching for year=$year and month=$month")

        val credit = dbHelper.getTotalForMonthByTypeFromExpenseUsingLike(month, year, "credit", username)
        val debit = dbHelper.getTotalForMonthByTypeFromExpenseUsingLike(month, year, "debit", username)
        val total = credit + debit

        totalSpentText.text = "Total Transaction in $monthLabel: ₹%.2f".format(total)
        creditDebitText.text = "Credit: ₹%.2f | Debit: ₹%.2f".format(credit, debit)

        updatePieChart(credit.toFloat(), debit.toFloat())
    }


    private fun updatePieChart(credit: Float, debit: Float) {
        val entries = mutableListOf<PieEntry>()
        if (credit > 0) entries.add(PieEntry(credit, "Credit"))
        if (debit > 0) entries.add(PieEntry(debit, "Debit"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.GREEN, Color.RED)
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD
        dataSet.valueTextSize = 16f
        dataSet.sliceSpace = 2f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false)
        pieChart.legend.isEnabled = true

        pieChart.setHoleColor(Color.BLACK)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.legend.textColor = Color.WHITE
        pieChart.setHoleColor(Color.BLACK)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.legend.textColor = Color.WHITE
        pieChart.data = pieData
        pieChart.invalidate()
        pieChart.notifyDataSetChanged()

    }
    override fun onResume() {
        super.onResume()
        val chip = selectedChip ?: monthChipGroup.getChildAt(0) as? Chip
        chip?.text?.toString()?.let { loadTotalSpent(it) }
    }



}
