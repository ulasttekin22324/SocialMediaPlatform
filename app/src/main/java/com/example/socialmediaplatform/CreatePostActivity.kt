package com.example.socialmediaplatform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreatePostActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: StorageReference
    private lateinit var contentEditText: EditText
    private lateinit var selectImageButton: Button
    private lateinit var shareButton: Button
    private var imageUri: Uri? = null
    private lateinit var selectImageActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("posts")
        storage = FirebaseStorage.getInstance().reference.child("post_images")
        contentEditText = findViewById(R.id.contentEditText)
        selectImageButton = findViewById(R.id.selectImageButton)
        shareButton = findViewById(R.id.shareButton)

        selectImageActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    imageUri = data?.data
                }
            }


        selectImageButton.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            selectImageActivityResultLauncher.launch(Intent.createChooser(intent, "Choose a photo"))
        }


        shareButton.setOnClickListener {
            val content = contentEditText.text.toString().trim()
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val username = auth.currentUser?.displayName ?: "Unknown User"

            if (content.isNotEmpty()) {
                val postId = database.push().key ?: return@setOnClickListener


                if (imageUri != null) {
                    val filePath = storage.child("$postId.jpg")
                    filePath.putFile(imageUri!!)
                        .addOnSuccessListener {
                            filePath.downloadUrl.addOnSuccessListener { uri ->
                                savePostToDatabase(
                                    postId,
                                    userId,
                                    username,
                                    content,
                                    uri.toString()
                                )
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Photo load error: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    savePostToDatabase(postId, userId, username, content, "")
                }
            } else {
                Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

    }

    private fun savePostToDatabase(
        postId: String,
        userId: String,
        username: String,
        content: String,
        imageUrl: String
    ) {
        val post = Post(postId, userId, username, content, imageUrl)


        database.child(postId).setValue(post)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val userPostsRef =
                        FirebaseDatabase.getInstance().getReference("user_posts").child(userId)
                    userPostsRef.child(postId).setValue(post)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Posted", Toast.LENGTH_SHORT)
                                    .show()
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Profile couldn't updated: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Post error: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
