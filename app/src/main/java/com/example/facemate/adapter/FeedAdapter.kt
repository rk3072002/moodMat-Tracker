package com.example.facemate.adapter



import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.facemate.R
import com.example.facemate.model.FeedPost


class FeedAdapter(private val posts: List<Pair<String, FeedPost>>, private val currentUserId: String, private val onLikeClicked: (String) -> Unit, private val onDeleteClicked: (String) -> Unit) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    inner class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tvMood)
        val likeBtn: ImageView = view.findViewById(R.id.ivLike)!!
        val likeCount: TextView = view.findViewById(R.id.tvLikeCount)!!
        val deleteBtn: ImageView = view.findViewById(R.id.ivDelete)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
       // val (key, post) = posts[position]
        val pair = posts[position]
        val key = pair.first
        val post = pair.second

        holder.textView.text = post.text
        holder.likeCount.text = post.likes.toString()

        val liked = post.likedUsers?.containsKey(currentUserId) == true
        val icon = if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        holder.likeBtn.setImageResource(icon)

        // Show delete only for own posts
        if (post.userId == currentUserId) {
            holder.deleteBtn.visibility = View.VISIBLE
        } else {
            holder.deleteBtn.visibility = View.GONE
        }

        holder.deleteBtn.setOnClickListener {
            onDeleteClicked(key)
        }

        holder.likeBtn.setOnClickListener {
            animateHeart(holder.likeBtn)
            onLikeClicked(key)
        }
    }


    private fun animateHeart(likeBtn: ImageView) {
        likeBtn.animate()
        .scaleX(1.2f)
        .scaleY(1.2f)
        .setDuration(120)
        .withEndAction {
            likeBtn.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .start()
        }.start()

    }

    override fun getItemCount() = posts.size
}
