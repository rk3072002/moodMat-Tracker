package com.example.facemate.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.facemate.MainActivity
import com.example.facemate.adapter.QuoteAdapter
import com.example.facemate.databinding.FragmentQuoteBinding
import com.example.facemate.model.Quote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class QuoteFragment : Fragment() {

    private lateinit var binding: FragmentQuoteBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: QuoteAdapter

    private var allQuotes = mutableListOf<Quote>()
    private var showOnlyMine = false
    private lateinit var currentUser: String
    private lateinit var currentUid: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuoteBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("quotes")
        currentUser = auth.currentUser?.email?.replace(".", "_") ?: "anonymous_user"
        currentUid = auth.currentUser?.uid ?: ""

        adapter = QuoteAdapter(listOf(), currentUser,
            onLike = { updateReaction(it.id, "likes") },
            onDislike = { updateReaction(it.id, "dislikes") },
            onDelete = { deleteQuote(it.id) }
        )

        binding.recyclerQuotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerQuotes.adapter = adapter

        fetchQuotes()

        binding.btnUpload.setOnClickListener {
            val quoteText = binding.editQuote.text.toString().trim()
            if (quoteText.isNotEmpty()) uploadQuote(quoteText)
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterQuotes()
            }
        })

        binding.btnFilter.setOnClickListener {
            showOnlyMine = !showOnlyMine
            filterQuotes()
            binding.btnFilter.text = if (showOnlyMine) "Show All Quotes" else "Show My Quotes Only"
        }

        return binding.root
    }

    private fun fetchQuotes() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allQuotes.clear()
                for (child in snapshot.children) {
                    val isReadBySnapshot = child.child("isReadBy")
                    val isReadBy = mutableMapOf<String, Boolean>()
                    for (user in isReadBySnapshot.children) {
                        isReadBy[user.key!!] = user.getValue(Boolean::class.java) ?: false
                    }

                    val quote = Quote(
                        id = child.key ?: "",
                        text = child.child("text").getValue(String::class.java) ?: "",
                        author = child.child("author").getValue(String::class.java) ?: "",
                        likes = child.child("likes").getValue(Int::class.java) ?: 0,
                        dislikes = child.child("dislikes").getValue(Int::class.java) ?: 0,
                        isReadBy = isReadBy
                    )
                    allQuotes.add(quote)
                }
                filterQuotes()
                updateUnreadCountBadge()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUnreadCountBadge() {
        val uid = currentUid
        val unreadCount = allQuotes.count {
            it.author != currentUser && !(it.isReadBy[uid] ?: false)
        }

        (activity as? MainActivity)?.updateNotificationBadge(unreadCount)
    }

    private fun filterQuotes() {
        val keyword = binding.editSearch.text.toString().lowercase(Locale.getDefault())
        val filtered = allQuotes.filter {
            (!showOnlyMine || it.author == currentUser) &&
                    it.text.lowercase(Locale.getDefault()).contains(keyword)
        }
        adapter.updateList(filtered)
    }

    private fun uploadQuote(text: String) {
        val id = database.push().key!!
        val quote = mapOf(
            "text" to text,
            "author" to currentUser,
            "likes" to 0,
            "dislikes" to 0,
            "isReadBy" to mapOf(currentUid to true)
        )
        database.child(id).setValue(quote).addOnSuccessListener {
            binding.editQuote.text.clear()
        }
    }

    private fun updateReaction(id: String, field: String) {
        val ref = database.child(id).child(field)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val value = currentData.getValue(Int::class.java) ?: 0
                currentData.value = value + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    private fun deleteQuote(id: String) {
        database.child(id).removeValue()
    }

    private fun markAllQuotesAsRead() {
        val uid = currentUid
        for (quote in allQuotes) {
            if (quote.author != currentUser && !(quote.isReadBy[uid] ?: false)) {
                database.child(quote.id).child("isReadBy").child(uid).setValue(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        markAllQuotesAsRead()
    }
}