package com.example.facemate.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.facemate.adapter.MoodHistoryAdapter
import com.example.facemate.databinding.FragmentHistoryBinding
import com.example.facemate.model.MoodHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class History : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var adapter: MoodHistoryAdapter
    private val moodList = ArrayList<MoodHistory>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)


        setupRecyclerView()
        setupFilter()
        fetchMoodHistory()  // Default: All

        Log.d("MoodDebug", "Current UID: $currentUser")

        return binding.root
    }

    private fun setupRecyclerView() {

        adapter = MoodHistoryAdapter(moodList) { moodLog, docId ->
            // Long Click Delete
            deleteMood(docId)
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun setupFilter() {
        val spinner: Spinner = binding.spinnerMoodFilter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedMood = parent?.getItemAtPosition(position).toString()
                //  Mood show in TextView

                binding.tvSelectedMood.text = "Selected Mood: $selectedMood"

                // Firestore mood fetch with filter
                fetchMoodHistory(selectedMood)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    private fun fetchMoodHistory(filter: String = "All") {
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        var query = db.collection("moods")
            .whereEqualTo("uid", currentUser)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        if (filter != "All") {
            query = query.whereEqualTo("mood", filter)
        }
        Log.d("MoodDebug", "Fetching moods for $currentUser")
        query.get().addOnSuccessListener { snapshot ->
            moodList.clear()
            for (doc in snapshot) {
                val mood = doc.getString("mood") ?: ""
                val note = doc.getString("note") ?: ""
                val timestamp = doc.getTimestamp("timestamp")?.toDate()
                val id = doc.id
                moodList.add(MoodHistory(mood, note, timestamp, id))
                Log.d("MoodDebug", "Total moods fetched: ${snapshot.size()}")
            }
            adapter.notifyDataSetChanged()
        }
            .addOnFailureListener {
                Log.e("MoodDebug", "Error fetching data", it)
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteMood(docId: String) {
        db.collection("moods").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Mood deleted", Toast.LENGTH_SHORT).show()
                fetchMoodHistory(binding.spinnerMoodFilter.selectedItem.toString())
            }

    }
}