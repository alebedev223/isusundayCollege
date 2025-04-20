package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.isusundaycollege.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class SignUpActivity : AppCompatActivity() {
    private var binding:ActivitySignUpBinding?=null
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth= Firebase.auth
        binding?.btnRegister?.setOnClickListener(){registerUser()
        }
        binding?.tvBackToLogin?.setOnClickListener(){
            startActivity(Intent(this,LoginActivity::class.java))
        }
    }
    private fun registerUser(){
        val name=binding?.etName?.text.toString()
        val email=binding?.etEmail?.text.toString()
        val password = binding?.etPassword?.text.toString()
        if(validateForm(name,email,password)){
            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(){task->
                    if (task.isSuccessful)
                    {
                        Toast.makeText(this,"Успешная регистрация",Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    }else
                        Toast.makeText(this,"Регистрация не удалась",Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun validateForm(name:String,email:String,password:String?):Boolean{
        return when{
            TextUtils.isEmpty(name)->{
                binding?.tilName?.error="Введите имя"
                false
            }
            TextUtils.isEmpty(email)&&!Patterns.EMAIL_ADDRESS.matcher(email).matches()->{
                binding?.tilEmail?.error="Неверный паттерн почты"
                false
            }
            TextUtils.isEmpty(password)->{
                binding?.tilPassword?.error="Введите пароль"
                false
            }
            else -> {true}
        }
    }
}