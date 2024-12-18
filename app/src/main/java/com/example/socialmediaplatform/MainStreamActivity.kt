package com.example.socialmediaplatform.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaplatform.CreatePostActivity
import com.example.socialmediaplatform.LoginActivity
import com.example.socialmediaplatform.ProfileActivity
import com.example.socialmediaplatform.R
import com.example.socialmediaplatform.PostAdapter
import com.example.socialmediaplatform.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainStreamActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: ArrayList<Post>
    private lateinit var database: DatabaseReference
    private lateinit var uploadPostButton: Button
    private lateinit var viewProfileButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainstream)


        auth = FirebaseAuth.getInstance()


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postList = ArrayList()
        postAdapter = PostAdapter(this, postList)
        recyclerView.adapter = postAdapter
        uploadPostButton = findViewById(R.id.uploadPostButton)


        uploadPostButton.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        viewProfileButton = findViewById(R.id.viewProfileButton)
        viewProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        database = FirebaseDatabase.getInstance().getReference("posts")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainStreamActivity, "Can't get the data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
