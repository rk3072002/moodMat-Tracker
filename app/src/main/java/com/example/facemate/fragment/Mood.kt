package com.example.facemate.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.facemate.R
import com.example.facemate.databinding.FragmentMoodBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Mood : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var binding: FragmentMoodBinding
    private lateinit var db: FirebaseFirestore
    private var selectedMood: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMoodBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        //  EditText scrollable
        binding.etNote.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance())

        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        //  Load images from Firestore into slider
        val imageList = ArrayList<SlideModel>()
//        imageList.add(SlideModel("https://res.cloudinary.com/demo/image/upload/sample.jpg", ScaleTypes.FIT))
//        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)
        db.collection("yoga_images")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        imageList.add(SlideModel(imageUrl, ScaleTypes.FIT))
                    }
                }
                binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Image load failed", Toast.LENGTH_SHORT).show()
            }

        //Mood Selection

        binding.btnHappy.setOnClickListener{
            selectedMood = "Happy"
            Toast.makeText(requireContext(), "Mood: 😊", Toast.LENGTH_SHORT).show()
        }

        binding.btnSad.setOnClickListener {
            selectedMood = "Sad"
            Toast.makeText(requireContext(), "Mood: 😢", Toast.LENGTH_SHORT).show()
        }
        binding.btnAngry.setOnClickListener {
            selectedMood = "Angry"
            Toast.makeText(requireContext(), "Mood: 😡", Toast.LENGTH_SHORT).show()
        }

        // Save Mood
        binding.btnSaveMood.setOnClickListener({
            val userNote = binding.etNote.text.toString().trim()
            val currentTime = Timestamp.now()

            if(selectedMood == null){
                Toast.makeText(requireContext(), "Please select a mood", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val moodData = hashMapOf(
                "uid" to currentUser,
                "mood" to selectedMood,
                "note" to userNote,
                "timestamp" to currentTime
            )

            db.collection("moods")
                .add(moodData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
                    binding.etNote.setText("")
                    selectedMood = null
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save mood.", Toast.LENGTH_SHORT).show()
                }
        })
        return binding.root

    }


    }
