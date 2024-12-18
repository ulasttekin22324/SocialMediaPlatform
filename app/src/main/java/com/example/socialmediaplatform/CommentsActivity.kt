package com.example.socialmediaplatform

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaplatform.ui.MainStreamActivity
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth


class CommentsActivity : AppCompatActivity() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var commentList: MutableList<Comment>
    private lateinit var postCommentButton: Button
    private lateinit var commentInput: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var goToMainButton: Button  // Ana Sayfaya Dön Butonu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)


        auth = FirebaseAuth.getInstance()


        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentInput = findViewById(R.id.commentInput)
        postCommentButton = findViewById(R.id.postCommentButton)
        goToMainButton = findViewById(R.id.goToMainButton)
        commentList = mutableListOf()


        val postId = intent.getStringExtra("postId") ?: return
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    comment?.let { commentList.add(it) }
                }
                commentsAdapter = CommentsAdapter(commentList)
                commentsRecyclerView.adapter = commentsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CommentsActivity, "Yorumlar alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        postCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()

            if (commentText.isNotEmpty()) {
                val commentId = commentsRef.push().key ?: return@setOnClickListener
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val username = auth.currentUser?.displayName ?: "Unknown"

                val comment = Comment(
                    commentId = commentId,
                    postId = postId,
                    userId = userId,
                    username = username,
                    content = commentText
                )

                commentsRef.child(commentId).setValue(comment)
                    .addOnSuccessListener {
                        commentInput.text.clear()
                        Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Comment error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Comment section cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }


        goToMainButton.setOnClickListener {
            val intent = Intent(this, MainStreamActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
