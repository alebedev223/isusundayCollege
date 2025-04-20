package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
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

class testActivity : AppCompatActivity() {

    private var binding: ActivityTestBinding? = null
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth = Firebase.auth

        binding?.coursesCard?.setOnClickListener(){
            startActivity(Intent(this, CoursesActivity::class.java))
            finish()
        }

        binding?.logoutCard?.setOnClickListener(){
            if (auth.currentUser!=null){
                auth.signOut()
                startActivity(Intent(this,LoginActivity::class.java))
                finish()
            }
        }
    }
}