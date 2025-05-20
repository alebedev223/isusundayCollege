package com.example.isusundaycollege

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.isusundaycollege.databinding.ActivityProfileBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profile: ImageView
    private lateinit var nameTV: TextView
    private lateinit var emailTV: TextView
    private lateinit var phoneTV: TextView
    private lateinit var roleTV: TextView
    private lateinit var editProfile: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Профиль пользователя"

        profile = findViewById(R.id.profileImageView)
        nameTV = findViewById(R.id.nameTextView)
        emailTV = findViewById(R.id.emailTextView)
        phoneTV = findViewById(R.id.phoneTextView)
        roleTV = findViewById(R.id.roleTextView)
        editProfile = findViewById(R.id.editProfileButton)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@ProfileActivity, testActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        })
        loadUserProfile()
        setupToolbar()
        editProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Профиль пользователя"
        binding.toolbar.setNavigationOnClickListener {
            val intent = Intent(this@ProfileActivity, testActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        emailTV.text = currentUser.email

        val userId = currentUser.uid
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Нет данных"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "Нет данных"
                    val role = snapshot.child("role").getValue(String::class.java) ?: "Студент"
                    nameTV.text = name
                    phoneTV.text = phone
                    roleTV.text = role

                    profile.setImageResource(R.drawable.default_profile_image)
                    
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileActivity", "Ошибка загрузки профиля: ${error.message}")
                Toast.makeText(this@ProfileActivity, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show()
            }
        })
    }

}