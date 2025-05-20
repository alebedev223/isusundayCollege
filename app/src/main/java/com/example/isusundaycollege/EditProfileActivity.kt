package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var profileImg: ImageView
    private lateinit var nameET: TextInputEditText
    private lateinit var emailET: TextInputEditText
    private lateinit var phoneET: TextInputEditText
    private lateinit var saveBtn: MaterialButton
    private lateinit var cancelBtn: MaterialButton

    private var isFromRegistration = false

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        isFromRegistration = intent.getBooleanExtra("FROM_REGISTRATION", false)
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (isFromRegistration) {
            supportActionBar?.title = "Заполните свой профиль"
        } else {
            supportActionBar?.title = "Редактирование профиля"
        }
        profileImg = findViewById(R.id.profileImageView)
        nameET = findViewById(R.id.nameEditText)
        emailET = findViewById(R.id.emailEditText)
        phoneET = findViewById(R.id.phoneEditText)
        saveBtn = findViewById(R.id.saveButton)
        cancelBtn = findViewById(R.id.cancelButton)
        loadUserData()
        saveBtn.setOnClickListener {
            saveUserProfile()
        }

        cancelBtn.setOnClickListener {
            if (isFromRegistration) {
                startActivity(Intent(this, testActivity::class.java))
            }
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isFromRegistration) {
                    startActivity(Intent(this, testActivity::class.java))
                    finish()
                } else {
                    onBackPressed()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        emailET.setText(currentUser.email)
        val userId = currentUser.uid
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    nameET.setText(name)
                    phoneET.setText(phone)
                    profileImg.setImageResource(R.drawable.default_profile_image)

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Ошибка загрузки данных профиля: ${error.message}")
                Toast.makeText(this@EditProfileActivity, "Ошибка загрузки данных профиля", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val name = nameET.text.toString().trim()
        val phone = phoneET.text.toString().trim()

        if (name.isEmpty()) {
            nameET.error = "Введите имя"
            return
        }
        Toast.makeText(this, "Сохранение профиля...", Toast.LENGTH_SHORT).show()
        saveUserDataToDatabase(userId, name, phone, null)

    }

    private fun saveUserDataToDatabase(userId: String, name: String, phone: String, newPhotoUrl: String?) {
        val userRef = database.child("users").child(userId)
        if (newPhotoUrl == null) {
            userRef.child("photoUrl").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPhotoUrl = snapshot.getValue(String::class.java)
                    updateUserData(userId, name, phone, currentPhotoUrl)
                }

                override fun onCancelled(error: DatabaseError) {
                    updateUserData(userId, name, phone, null)
                }
            })
        } else {
            updateUserData(userId, name, phone, newPhotoUrl)
        }
    }

    private fun updateUserData(userId: String, name: String, phone: String, photoUrl: String?) {
        val userUpdates = HashMap<String, Any>()
        userUpdates["name"] = name
        userUpdates["phone"] = phone


        if (photoUrl != null) {
            userUpdates["photoUrl"] = photoUrl
        }

        database.child("users").child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show()
                if (isFromRegistration) {
                    startActivity(Intent(this, testActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка обновления профиля: ${e.message}")
                Toast.makeText(this, "Не удалось обновить профиль", Toast.LENGTH_SHORT).show()
            }
    }

}