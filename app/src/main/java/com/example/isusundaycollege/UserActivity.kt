package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.isusundaycollege.databinding.ActivityUserBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: UserAdapter
    private val usersList = mutableListOf<UserModel>()
    private val roles = listOf("Администратор", "Преподаватель", "Студент")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        val fbDb =
            FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")

        dbRef = fbDb.reference
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@UserActivity, testActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        })
        setupToolbar()
        checkAdminAccess()
        setupRecyclerView()
        loadUsers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Управление пользователями"
        binding.toolbar.setNavigationOnClickListener { val intent = Intent(this@UserActivity, testActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()}
    }

    private fun checkAdminAccess() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbRef.child("users").child(currentUser.uid).get().addOnSuccessListener { snapshot ->
            val role = snapshot.child("role").getValue(String::class.java)
            if (role != "Администратор") {
                Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка при проверке прав доступа", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(usersList) { userId, newRole ->
            updateUserRole(userId, newRole)
        }
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usersRecyclerView.adapter = adapter
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        dbRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Нет имени"
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                    val phone = userSnapshot.child("phone").getValue(String::class.java) ?: ""
                    val role = userSnapshot.child("role").getValue(String::class.java) ?: "Студент"

                    usersList.add(UserModel(userId, name, email, phone, role))
                }
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE

                if (usersList.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.emptyView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserActivity, "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateUserRole(userId: String, newRole: String) {
        dbRef.child("users").child(userId).child("role").setValue(newRole)
            .addOnSuccessListener {
                Toast.makeText(this, "Роль обновлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class UserModel(
        val id: String,
        val name: String,
        val email: String,
        val phone: String,
        val role: String
    )

    inner class UserAdapter(
        private val users: List<UserModel>,
        private val onRoleChanged: (String, String) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
            val phoneTextView: TextView = itemView.findViewById(R.id.phoneTextView)
            val roleSpinner: androidx.appcompat.widget.AppCompatSpinner = itemView.findViewById(R.id.roleSpinner)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]

            holder.nameTextView.text = user.name
            holder.emailTextView.text = if (user.email.isNotEmpty()) user.email else "Нет email"
            holder.phoneTextView.text = if (user.phone.isNotEmpty()) user.phone else "Нет телефона"

            val adapter = ArrayAdapter(this@UserActivity, android.R.layout.simple_spinner_dropdown_item, roles)
            holder.roleSpinner.adapter = adapter

            val rolePosition = roles.indexOf(user.role)
            if (rolePosition != -1) {
                holder.roleSpinner.setSelection(rolePosition)
            }

            holder.roleSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    val selectedRole = roles[pos]
                    if (selectedRole != user.role) {
                        onRoleChanged(user.id, selectedRole)
                    }
                }

                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {

                }
            })
            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(this@UserActivity, EditUserActivity::class.java).apply {
                    putExtra("USER_ID", user.id)
                    putExtra("USER_NAME", user.name)
                    putExtra("USER_EMAIL", user.email)
                    putExtra("USER_PHONE", user.phone)
                    putExtra("USER_ROLE", user.role)
                }
                this@UserActivity.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = users.size
    }
}