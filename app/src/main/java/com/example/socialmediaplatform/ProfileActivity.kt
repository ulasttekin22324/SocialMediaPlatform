package com.example.socialmediaplatform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaplatform.ui.MainStreamActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private val IMAGE_PICK_CODE = 1000
    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var biographyTextView: TextView
    private lateinit var editButton: Button
    private lateinit var goToMainPageButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private var selectedImageUri: Uri? = null
    private val postList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()


        databaseReference = FirebaseDatabase.getInstance().getReference("user_profiles")
        storageReference = FirebaseStorage.getInstance().getReference("profile_images")

        profileImageView = findViewById(R.id.profileImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        biographyTextView = findViewById(R.id.biographyTextView)
        editButton = findViewById(R.id.editButton)
        goToMainPageButton = findViewById(R.id.goToMainPageButton)
        postsRecyclerView = findViewById(R.id.postsRecyclerView)

        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(this, postList)
        postsRecyclerView.adapter = postAdapter

        loadUserProfile()

        editButton.setOnClickListener {
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
        }

        goToMainPageButton.setOnClickListener {
            val intent = Intent(this, MainStreamActivity::class.java)
            startActivity(intent)
        }

        loadUserPosts()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userProfile = snapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    usernameTextView.text = userProfile.username ?: "Kullanıcı adı bulunamadı"
                    biographyTextView.text = userProfile.biography ?: "Biyografi bulunamadı"

                    if (userProfile.profileImageUrl.isNotEmpty()) {
                        Glide.with(this@ProfileActivity).load(userProfile.profileImageUrl).into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile_image)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Can't get the data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return

        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()  // Clear the existing list
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            uploadProfileImageToFirebase()
        }
    }

    private fun uploadProfileImageToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val fileReference = storageReference.child("$userId.jpg")

        selectedImageUri?.let {
            fileReference.putFile(it)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val profileImageUrl = uri.toString()
                        databaseReference.child(userId).child("profileImageUrl").setValue(profileImageUrl)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile pic has changed!", Toast.LENGTH_SHORT).show()
                                Glide.with(this).load(profileImageUrl).into(profileImageView)
                            }
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Photo couldn't load: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
