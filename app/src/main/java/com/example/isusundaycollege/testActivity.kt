package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.isusundaycollege.databinding.ActivityMainBinding
import com.example.isusundaycollege.databinding.ActivityTestBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class testActivity : AppCompatActivity() {

    private var binding: ActivityTestBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app")
        auth = Firebase.auth

        binding?.coursesCard?.setOnClickListener(){
            startActivity(Intent(this, CoursesActivity::class.java))
            finish()
        }

        binding?.profileCard?.setOnClickListener(){
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        binding?.settingsCard?.setOnClickListener(){
            if (auth.currentUser!=null) {
                auth.signOut()
                auth.addAuthStateListener { firebaseAuth ->
                    if (firebaseAuth.currentUser == null) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }else{
                startActivity(Intent(this,LoginActivity::class.java))
                finish()
            }
        }
        binding?.scheduleCard?.setOnClickListener(){
            startActivity(Intent(this,ScheduleActivity::class.java))
            finish()
        }
        checkAccess()
    }
    private fun checkAccess(){
        val curUser = auth.currentUser ?: return

        database.reference.child("users").child(curUser.uid).get()
            .addOnSuccessListener { snap ->
                val role = snap.child("role").getValue(String::class.java)

                if (role == "Администратор") {
                    binding?.usersCard?.visibility = View.VISIBLE
                    binding?.groupsCard?.visibility=View.VISIBLE
                    binding?.groupsCard?.setOnClickListener(){
                        startActivity(Intent(this, GroupActivity::class.java))
                        finish()
                    }
                    binding?.usersCard?.setOnClickListener {
                        startActivity(Intent(this, UserActivity::class.java))
                        finish()
                    }
                } else {
                    binding?.usersCard?.visibility = View.GONE
                    binding?.groupsCard?.visibility=View.GONE

                }
            }
            .addOnFailureListener { e ->
                println("Ошибка при получении данных пользователя: ${e.message}")
            }
    }
}