package com.example.socialmediaplatform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreateProfileActivity : AppCompatActivity() {
    private lateinit var profileImageView: ImageView
    private lateinit var usernameEditText: EditText
    private lateinit var biographyEditText: EditText
    private lateinit var selectProfileImageButton: Button
    private lateinit var saveProfileButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null
    private var existingProfileImageUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("user_profiles")
        storageReference = FirebaseStorage.getInstance().getReference("profile_images")

        profileImageView = findViewById(R.id.profileImageView)
        usernameEditText = findViewById(R.id.usernameEditText)
        biographyEditText = findViewById(R.id.biographyEditText)
        selectProfileImageButton = findViewById(R.id.selectProfileImageButton)
        saveProfileButton = findViewById(R.id.createProfileButton)

        loadUserProfile()

        selectProfileImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1000)
        }

        saveProfileButton.setOnClickListener {
            saveProfileToDatabase()
        }
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value as? String
                    existingProfileImageUrl = snapshot.child("profileImageUrl").value as? String
                    val biography = snapshot.child("biography").value as? String

                    usernameEditText.setText(username ?: "")
                    biographyEditText.setText(biography ?: "")
                    existingProfileImageUrl?.let {
                        Glide.with(this@CreateProfileActivity)
                            .load(it)
                            .into(profileImageView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CreateProfileActivity, "Can't load the profile data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfileToDatabase() {
        val userId = auth.currentUser?.uid ?: return
        val username = usernameEditText.text.toString().trim()
        val biography = biographyEditText.text.toString().trim()


        selectedImageUri?.let { uri ->
            val fileReference = storageReference.child("$userId.jpg")
            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        val profileImageUrl = downloadUri.toString()

                        val userProfile = UserProfile(username, biography, profileImageUrl)

                        val userProfileMap = mapOf(
                            "username" to userProfile.username,
                            "biography" to userProfile.biography,
                            "profileImageUrl" to userProfile.profileImageUrl
                        )

                        database.child(userId).setValue(userProfileMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile has successfully saved", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, ProfileActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Profile couldn't saved", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Profile pic loaded", Toast.LENGTH_SHORT).show()
                }
        } ?: run {

            val profileImageUrl = existingProfileImageUrl ?: ""

            val userProfile = UserProfile(username, biography, profileImageUrl)

            val userProfileMap = mapOf(
                "username" to userProfile.username,
                "biography" to userProfile.biography,
                "profileImageUrl" to userProfile.profileImageUrl
            )

            database.child(userId).setValue(userProfileMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile successfully saved", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Profile couldn't saved", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            Glide.with(this).load(selectedImageUri).into(profileImageView)
        }
    }
}
