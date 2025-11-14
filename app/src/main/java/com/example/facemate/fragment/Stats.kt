package com.example.facemate.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.facemate.R
import com.example.facemate.databinding.FragmentStatsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class Stats : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filters = listOf("All", "Weekly", "Monthly")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filters)
        binding.filterSpinner.adapter = adapter

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                val selected = filters[position]
                binding.selectedFilterText.setText(selected) // Set selected filter
                fetchAndShowData(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        fetchAndShowData("All")
    }

    private fun fetchAndShowData(filter: String) {

        if (userId == null) {
            binding.moodSummaryText.text = "User not logged in"
            return
        }
        val moodCountMap = mutableMapOf<String, Int>()
        val barDataMap = LinkedHashMap<String, MutableMap<String, Int>>()

        db.collection("moods")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val mood = doc.getString("mood") ?: continue
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue

                    if (!shouldInclude(filter, timestamp)) continue

                    moodCountMap[mood] = moodCountMap.getOrDefault(mood, 0) + 1


                    val label = when (filter) {
                        "Weekly" -> SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp)
                        "Monthly" -> SimpleDateFormat("MMM", Locale.getDefault()).format(timestamp)
                        else -> dateFormat.format(timestamp)
                    }

                    barDataMap[label] = barDataMap[label] ?: mutableMapOf()
                    barDataMap[label]!![mood] = barDataMap[label]!!.getOrDefault(mood, 0) + 1
                }

                showMoodSummary(moodCountMap)
                showPieChart(moodCountMap)
                showBarChart(barDataMap)
            }
    }

    private fun shouldInclude(filter: String, date: Date): Boolean {
        val calendar = Calendar.getInstance()
        when (filter) {
            "Weekly" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                return date.after(calendar.time)
            }

            "Monthly" -> {
                calendar.add(Calendar.MONTH, -1)
                return date.after(calendar.time)
            }
        }
        return true
    }

    private fun showMoodSummary(moodMap: Map<String, Int>) {
        val summaryText = if (moodMap.isEmpty()) {
            "No mood data available."
        } else {
            moodMap.entries.joinToString(" | ") { "${it.key}: ${it.value}" }
        }
        binding.moodSummaryText.text = summaryText
    }

    private fun showPieChart(moodMap: Map<String, Int>) {
        val entries = moodMap.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Mood Distribution").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextColor = Color.BLACK
            valueTextSize = 14f
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            centerText = "Mood Pie"
            animateY(1000)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(14f)
            invalidate()

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val pieEntry = e as? PieEntry
                    pieEntry?.let {
                        binding.moodSummaryText.text =
                            "Selected Mood: ${it.label} (${it.value.toInt()} times)"
                    }
                }

                override fun onNothingSelected() {
                    showMoodSummary(moodMap)
                }
            })

            invalidate()
        }
    }

    private fun showBarChart(barMap: Map<String, Map<String, Int>>) {
        val moodColors = mapOf(
            "Happy" to ColorTemplate.COLORFUL_COLORS[0],
            "Sad" to ColorTemplate.COLORFUL_COLORS[1],
            "Angry" to ColorTemplate.COLORFUL_COLORS[2],
            "Neutral" to ColorTemplate.COLORFUL_COLORS[3]
        )
        val xLabels = barMap.keys.toList()
        val moodSet = barMap.values.flatMap { it.keys }.toSet()
        val dataSets = mutableListOf<IBarDataSet>()



        moodSet.forEach { mood ->
            val entries = xLabels.mapIndexed { index, label ->
                val count = barMap[label]?.get(mood) ?: 0
                BarEntry(index.toFloat(), count.toFloat())
            }
            val dataSet = BarDataSet(entries, mood).apply {
                color = moodColors[mood] ?: Color.GRAY
                valueTextSize = 12f
            }
            dataSets.add(dataSet)
        }

        val barData = BarData(dataSets)
        barData.barWidth = 0.2f

        binding.barChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            animateY(1000)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(xLabels)
                granularity = 1f
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawLabels(true)
            }
            legend.isEnabled = true
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Stats.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Stats().apply {
                arguments = Bundle().apply {

                }
            }
    }
}