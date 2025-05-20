package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.isusundaycollege.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {
    private var binding:ActivityMainBinding?=null
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth=Firebase.auth

        /*val scheduleCard=findViewById<Button>(R.id.scheduleCard)
        scheduleCard.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        val coursesCard=findViewById<Button>(R.id.coursesCard)
        coursesCard.setOnClickListener {
            startActivity(Intent(this, CoursesActivity::class.java))
        }
        val profileCard=findViewById<Button>(R.id.profileCard)
        profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        val settingsCard=findViewById<Button>(R.id.settingsCard)
        settingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }*/
    }
}