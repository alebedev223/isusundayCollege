package com.example.isusundaycollege

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class StudentAddActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentsAdapter
    private val studentsList = mutableListOf<Student>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("students")

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Управление студентами"

        recyclerView = findViewById(R.id.recyclerStudents)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentsAdapter(studentsList) { student: Student ->
            showEditStudentDialog(student)
        }
        recyclerView.adapter = adapter

        loadStudents()

        val fab = findViewById<FloatingActionButton>(R.id.fabAddStudent)
        fab.setOnClickListener {
            showAddStudentDialog()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadStudents() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentsList.clear()
                for (studentSnapshot in snapshot.children) {
                    val studentId = studentSnapshot.key ?: continue
                    val studentName = studentSnapshot.child("name").getValue(String::class.java) ?: "Имя не найдено"
                    val student = Student(studentId, studentName)
                    studentsList.add(student)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentAddActivity, "Ошибка загрузки студентов: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddStudentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_student, null)
        val editTextStudentName = dialogView.findViewById<TextInputEditText>(R.id.editTextStudentName)
        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить студента")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val studentName = editTextStudentName.text.toString().trim()
                if (studentName.isNotEmpty()) {
                    addStudent(studentName)
                } else {
                    Toast.makeText(this, "Имя студента не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addStudent(studentName: String) {
        val studentId = database.push().key ?: return
        val studentData = hashMapOf(
            "name" to studentName
        )

        database.child(studentId).setValue(studentData)
            .addOnSuccessListener {
                Toast.makeText(this, "Студент добавлен", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка добавления студента: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditStudentDialog(student: Student) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_student, null)
        val editTextStudentName = dialogView.findViewById<TextInputEditText>(R.id.editTextStudentName)

        editTextStudentName.setText(student.name)

        MaterialAlertDialogBuilder(this)
            .setTitle("Редактировать студента")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editTextStudentName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateStudent(student.id, newName)
                } else {
                    Toast.makeText(this, "Имя студента не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ ->
                deleteStudent(student)
            }
            .show()
    }

    private fun updateStudent(studentId: String, newName: String) {
        val studentData = hashMapOf(
            "name" to newName
        )

        database.child(studentId).setValue(studentData)
            .addOnSuccessListener {
                Toast.makeText(this, "Студент обновлен", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка обновления студента: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteStudent(student: Student) {
        database.child(student.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Студент удален", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления студента: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}