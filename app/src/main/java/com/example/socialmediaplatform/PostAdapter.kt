package com.example.socialmediaplatform

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class PostAdapter(private val context: Context, private val postList: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        val postImageView: ImageView = itemView.findViewById(R.id.postImageView)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val commentButton: ImageView = itemView.findViewById(R.id.commentButton)
        val likeCountTextView: TextView = itemView.findViewById(R.id.likeCountTextView)
        val commentCountTextView: TextView = itemView.findViewById(R.id.commentCountTextView)
        val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.usernameTextView.text = post.username
        holder.contentTextView.text = post.content
        holder.likeCountTextView.text = "${post.likes} Likes"

        updateCommentCount(post.postId, holder.commentCountTextView)

        if (post.imageUrl.isNotEmpty()) {
            Glide.with(context).load(post.imageUrl).into(holder.postImageView)
            holder.postImageView.visibility = View.VISIBLE
        } else {
            holder.postImageView.visibility = View.GONE
        }

        holder.likeButton.setOnClickListener {
            handleLikeClick(post.postId, holder.likeCountTextView)
        }

        holder.commentButton.setOnClickListener {
            val intent = Intent(context, CommentsActivity::class.java).apply {
                putExtra("postId", post.postId)
            }
            context.startActivity(intent)
        }

        holder.optionsButton.setOnClickListener {
            if (post.userId == FirebaseAuth.getInstance().currentUser?.uid) {
                showOptionsMenu(holder.optionsButton, post.postId, position)
            }
        }
    }

    override fun getItemCount(): Int = postList.size

    private fun showOptionsMenu(view: View, postId: String, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.post_options_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.deletePost) {
                deletePost(postId, position)
                true
            } else {
                false
            }
        }
        popupMenu.show()
    }

    private fun deletePost(postId: String, position: Int) {
        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId)
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)

        postRef.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                commentsRef.removeValue()
                notifyItemRemoved(position)
            }
        }
    }

    private fun updateCommentCount(postId: String, commentCountTextView: TextView) {
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentCount = snapshot.childrenCount
                commentCountTextView.text = "$commentCount Comments"
            }

            override fun onCancelled(error: DatabaseError) {
                commentCountTextView.text = "0 Comments"
            }
        })
    }

    private fun handleLikeClick(postId: String, likeCountTextView: TextView) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val likesRef = FirebaseDatabase.getInstance().getReference("likes").child(postId)
        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId).child("likes")

        likesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    likesRef.child(userId).removeValue()
                    postRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val currentLikes = mutableData.getValue(Int::class.java) ?: 0
                            if (currentLikes > 0) {
                                mutableData.value = currentLikes - 1
                            }
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                    })
                } else {
                    likesRef.child(userId).setValue(true)
                    postRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val currentLikes = mutableData.getValue(Int::class.java) ?: 0
                            mutableData.value = currentLikes + 1
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                likeCountTextView.text = "$currentLikes Likes"
            }

            override fun onCancelled(error: DatabaseError) {
                likeCountTextView.text = "0 Likes"
            }
        })
    }


}
