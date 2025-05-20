package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.isusundaycollege.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private var binding: ActivityLoginBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding?.loginButton?.setOnClickListener {
            loginUser()
        }

        binding?.registerButton?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        binding?.loginGoogleButton?.setOnClickListener {
            signInWithGoogle()
        }

        if (auth.currentUser != null) {
            startActivity(Intent(this, testActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginActivity", "аутентификация через google :" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "вход через google неудачный", e)
                Toast.makeText(this, "Ошибка входа через Google: код ${e.statusCode}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser() {
        val email = binding?.loginInput?.text.toString()
        val password = binding?.passwordInput?.text.toString()

        if (validateForm(email, password)) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Успешный логин", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, testActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Авторизация не удалась: Неверные данные",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "credential прошел")
                    val user = auth.currentUser
                    Toast.makeText(this, "Вход через Google выполнен успешно",
                        Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, testActivity::class.java))
                    finish()
                } else {
                    Log.w("LoginActivity", "ошибка входа через credential", task.exception)
                    Toast.makeText(this, "Ошибка входа через Google: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun validateForm(email: String, password: String?): Boolean {
        return when {
            TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding?.loginInput?.error = "Неверный формат почты"
                false
            }
            TextUtils.isEmpty(password) -> {
                binding?.passwordInput?.error = "Введите пароль"
                false
            }
            else -> true
        }
    }

    private fun updateUI(user: com.google.firebase.auth.FirebaseUser?) {
        if (user != null) {
            Log.d("LoginActivity", "Пользователь вошел в систему: ${user.email}")
        } else {
            Log.d("LoginActivity", "Пользователь вышел из системы")
        }
    }
}