package com.example.facemate.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.facemate.R
import com.example.facemate.adapter.FeedAdapter
import com.example.facemate.databinding.FragmentFeedBinding
import com.example.facemate.model.FeedPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener


import android.util.Pair
class Feed : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var databaseRef: DatabaseReference
    private val postList = mutableListOf<Pair<String, FeedPost>>()
    private lateinit var adapter: FeedAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        databaseRef = FirebaseDatabase.getInstance().getReference("posts")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = FeedAdapter(postList, currentUserId,
            onLikeClicked = { key -> incrementLike(key) },
            onDeleteClicked = { key -> deletePost(key) }
        )
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.feedRecyclerView.adapter = adapter

        binding.btnPost.setOnClickListener {
            val mood = binding.etMood.text.toString().trim()
            if (mood.isNotEmpty()) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val post = FeedPost(
                    text = mood,
                    likes = 0,
                    timestamp = System.currentTimeMillis(),
                    userId = userId
                )
                databaseRef.push().setValue(post)
                binding.etMood.text.clear()
            }
        }

        fetchPosts()
        return binding.root
    }

    private fun deletePost(key: String) {
        databaseRef.child(key).removeValue()
    }

    private fun fetchPosts() {
        databaseRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (snap in snapshot.children.reversed()) {
                    val post = snap.getValue(FeedPost::class.java)
                    post?.let {
                        postList.add(Pair(snap.key!!, it))
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun incrementLike(key: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postRef = databaseRef.child(key)


        postRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Likes field
                val likes = currentData.child("likes").getValue(Int::class.java) ?: 0

                // likedUsers field (Map<String, Boolean>)
                val likedUsersMap = currentData.child("likedUsers").value as? Map<String, Boolean> ?: emptyMap()

                // Check if user already liked
                if (likedUsersMap.containsKey(userId)) {
                    // Already liked → unlike
                    currentData.child("likes").value = if (likes > 0) likes - 1 else 0
                    currentData.child("likedUsers").child(userId).value = null
                } else {
                    // Not liked yet → like
                    currentData.child("likes").value = likes + 1
                    currentData.child("likedUsers").child(userId).value = true
                }

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {}
        })
    }
}