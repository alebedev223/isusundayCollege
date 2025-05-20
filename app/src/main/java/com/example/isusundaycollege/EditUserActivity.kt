package com.example.isusundaycollege

import android.R
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.isusundaycollege.databinding.ActivityEditUserBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditUserBinding
    private lateinit var dbRef: DatabaseReference
    private val roles = listOf("Администратор", "Преподаватель", "Студент")

    private var userId: String = ""
    private var userName: String = ""
    private var userEmail: String = ""
    private var userPhone: String = ""
    private var userRole: String = ""

    companion object {
        private const val TAG = "AdminEditUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fbDb = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = fbDb.reference

        userId = intent.getStringExtra("USER_ID") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        userPhone = intent.getStringExtra("USER_PHONE") ?: ""
        userRole = intent.getStringExtra("USER_ROLE") ?: "Студент"

        setupToolbar()
        setupRoleSpinner()
        fillUserData()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Редактирование пользователя"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRoleSpinner() {
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, roles)
        binding.roleSpinner.adapter = adapter

        val rolePosition = roles.indexOf(userRole)
        if (rolePosition != -1) {
            binding.roleSpinner.setSelection(rolePosition)
        }
    }

    private fun fillUserData() {
        binding.nameEditText.setText(userName)
        binding.emailEditText.setText(userEmail)
        binding.phoneEditText.setText(userPhone)

        binding.emailEditText.isEnabled = false
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveUserData()
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveUserData() {
        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val role = roles[binding.roleSpinner.selectedItemPosition]

        if (name.isEmpty()) {
            binding.nameEditText.error = "Введите имя"
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val userUpdates = HashMap<String, Any>()
        userUpdates["name"] = name
        userUpdates["phone"] = phone
        userUpdates["role"] = role

        dbRef.child("users").child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Данные пользователя обновлены", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Ошибка обновления данных: ${e.message}")
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
}