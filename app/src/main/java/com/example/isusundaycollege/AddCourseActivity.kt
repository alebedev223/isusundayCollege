package com.example.isusundaycollege

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddCourseActivity : AppCompatActivity() {

    private lateinit var courseNameEditText: TextInputEditText
    private lateinit var courseDescriptionEditText: TextInputEditText
    private lateinit var teacherSelectorDropdown: AutoCompleteTextView
    private lateinit var courseAvailabilitySwitch: SwitchMaterial
    private lateinit var saveCourseButton: MaterialButton
    private lateinit var progressBar: View

    private val teachersList = mutableListOf<TeacherData>()
    private var selectedTeacherId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_course)

        courseNameEditText = findViewById(R.id.courseNameEditText)
        courseDescriptionEditText = findViewById(R.id.courseDescriptionEditText)
        teacherSelectorDropdown = findViewById(R.id.teacherSelectorDropdown)
        courseAvailabilitySwitch = findViewById(R.id.courseAvailabilitySwitch)
        saveCourseButton = findViewById(R.id.saveCourseButton)
        progressBar = findViewById(R.id.progressBar)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        checkUserRole()
        loadTeachersList()
        saveCourseButton.setOnClickListener {
            validateAndSaveCourse()
        }
    }

    private fun checkUserRole() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid
        val database =
            FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference

        database.child("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userRole = snapshot.getValue(String::class.java) ?: "student"
                    if (userRole != "Администратор") {
                        Toast.makeText(
                            this@AddCourseActivity,
                            "Нет прав для добавления курсов",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddCourseActivity", "Ошибка проверки роли: ${error.message}")
                    Toast.makeText(
                        this@AddCourseActivity,
                        "Ошибка загрузки данных",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            })
    }

    private fun loadTeachersList() {
        showLoading(true)
        val database =
            FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference

        database.child("users")
            .orderByChild("role")
            .equalTo("Преподаватель")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    teachersList.clear()
                    teachersList.add(TeacherData("", "Преподаватель не назначен"))

                    for (teacherSnapshot in snapshot.children) {
                        val teacherId = teacherSnapshot.key ?: continue
                        val teacherName = teacherSnapshot.child("name").getValue(String::class.java)
                            ?: "Преподаватель"

                        teachersList.add(TeacherData(teacherId, teacherName))
                    }

                    setupTeachersDropdown()
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddCourseActivity", "Ошибка загрузки преподавателей: ${error.message}")
                    showLoading(false)
                    Toast.makeText(
                        this@AddCourseActivity,
                        "Ошибка загрузки списка преподавателей",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun setupTeachersDropdown() {
        val teacherNames = teachersList.map { it.name }
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, teacherNames)
        teacherSelectorDropdown.setAdapter(adapter)

        teacherSelectorDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedTeacherId = teachersList[position].id
        }
        if (teachersList.isNotEmpty()) {
            teacherSelectorDropdown.setText(teachersList[0].name, false)
            selectedTeacherId = teachersList[0].id
        }
    }

    private fun validateAndSaveCourse() {
        val courseName = courseNameEditText.text.toString().trim()
        val courseDescription = courseDescriptionEditText.text.toString().trim()
        val isAvailable = courseAvailabilitySwitch.isChecked
        if (courseName.isEmpty()) {
            courseNameEditText.error = "Введите название курса"
            return
        }

        if (courseDescription.isEmpty()) {
            courseDescriptionEditText.error = "Введите описание курса"
            return
        }
        saveCourse(courseName, courseDescription, selectedTeacherId, isAvailable)
    }

    private fun saveCourse(
        courseName: String,
        courseDescription: String,
        teacherId: String?,
        isAvailable: Boolean
    ) {
        showLoading(true)

        val database =
            FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val courseRef = database.child("courses").push()

        val courseMap = HashMap<String, Any>()
        courseMap["name"] = courseName
        courseMap["description"] = courseDescription
        courseMap["isAvailable"] = isAvailable

        if (!teacherId.isNullOrEmpty()) {
            courseMap["teacher"] = teacherId
        }

        courseRef.setValue(courseMap)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Курс успешно создан", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("AddCourseActivity", "Ошибка сохранения курса: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения курса", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            saveCourseButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            saveCourseButton.isEnabled = true
        }
    }

    data class TeacherData(val id: String, val name: String)
}