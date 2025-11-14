package com.example.facemate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.facemate.R
import com.example.facemate.model.Quote

class QuoteAdapter(
    private var quoteList: List<Quote>,
    private val currentUser: String,
    private val onLike: (Quote) -> Unit,
    private val onDislike: (Quote) -> Unit,
    private val onDelete: (Quote) -> Unit
) : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {

    inner class QuoteViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quoteList[position]

        holder.view.findViewById<TextView>(R.id.txtQuote).text = quote.text
        holder.view.findViewById<TextView>(R.id.txtAuthor).text = "by ${quote.author.replace("_", ".")}"
        holder.view.findViewById<TextView>(R.id.txtLikeCount).text = quote.likes.toString()
        holder.view.findViewById<TextView>(R.id.txtDislikeCount).text = quote.dislikes.toString()

        holder.view.findViewById<Button>(R.id.btnLike).setOnClickListener { onLike(quote) }
        holder.view.findViewById<Button>(R.id.btnDislike).setOnClickListener { onDislike(quote) }

        val btnDelete = holder.view.findViewById<Button>(R.id.btnDelete)
        if (quote.author == currentUser) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { onDelete(quote) }
        } else {
            btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = quoteList.size

    fun updateList(newList: List<Quote>) {
        quoteList = newList
        notifyDataSetChanged()
    }
}
