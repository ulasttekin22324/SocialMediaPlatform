package com.example.socialmediaplatform.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmediaplatform.CreateProfileActivity
import com.example.socialmediaplatform.R
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var welcomToMainPage: Button
    private lateinit var goToProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        welcomeTextView = findViewById(R.id.welcomeTextView)
        welcomToMainPage = findViewById(R.id.welcomeToMainPage)
        logoutButton = findViewById(R.id.logoutButton)
        goToProfileButton = findViewById(R.id.goToProfileButton)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val username = currentUser.displayName ?: "Unknown"
            val userId = currentUser.uid


            welcomeTextView.text = "Welcome, $username"
        } else {
            redirectToLogin()
        }


        welcomToMainPage.setOnClickListener {
            val intent = Intent(this, MainStreamActivity::class.java)
            startActivity(intent)
        }

        goToProfileButton.setOnClickListener {
            Log.d("HomeActivity", "Clicked to button")
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Successfully log out ", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
