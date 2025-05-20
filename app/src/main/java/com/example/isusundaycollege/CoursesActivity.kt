package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CoursesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressIndicator: CircularProgressIndicator
    private val coursesList = mutableListOf<Course>()
    private lateinit var adapter: CoursesAdapter
    private var userRole: String = "student"
    private var userId: String = ""
    private lateinit var addCourseButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses)

        FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) Log.d("CoursesActivity", "Firebase подключение активно")
                else Log.d("CoursesActivity", "Firebase подключение отсутсвует")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CoursesActivity", "Ошибка проверки подключения: ${error.message}")
            }
        })

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this@CoursesActivity, testActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@CoursesActivity, testActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        })

        recyclerView = findViewById(R.id.coursesRecyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)
        addCourseButton = findViewById(R.id.addCourseButton)

        addCourseButton.setOnClickListener {
            val intent = Intent(this, AddCourseActivity::class.java)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        adapter = CoursesAdapter(coursesList) { courseId ->
            val course = coursesList.find { it.id == courseId }
            if (course != null && course.isAvailable) {
                val intent = Intent(this, AttendanceActivity::class.java)
                intent.putExtra("COURSE_ID", courseId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Этот курс недоступен", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            checkUserRole()
        } else {
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkUserRole() {
        showLoading(true)
        val fbDb = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        val database = fbDb.reference

        database.child("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userRole = snapshot.getValue(String::class.java) ?: "student"
                    loadCourses()
                    addCourseButton.visibility = if (userRole == "Администратор" || userRole == "Преподаватель") {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CoursesActivity", "Ошибка проверки роли: ${error.message}")
                    showLoading(false)
                    Toast.makeText(this@CoursesActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadCourses() {
        Log.d("CoursesActivity", "Начало загрузки курсов")
        val fbDb = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        val database = fbDb.reference

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        database.child("courses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("CoursesActivity", "onDataChange вызван")

                if (!snapshot.exists()) {
                    Log.d("CoursesActivity", "Данные о курсах отсутствуют")
                    showLoading(false)
                    return
                }

                Log.d("CoursesActivity", "Количество курсов: ${snapshot.childrenCount}")

                coursesList.clear()

                for (courseSnapshot in snapshot.children) {
                    val courseId = courseSnapshot.key ?: continue
                    val name = courseSnapshot.child("name").getValue(String::class.java) ?: "Без названия"

                    val isCourseAvailable = courseSnapshot.child("isAvailable").getValue(Boolean::class.java) ?: true

                    val teacherId = courseSnapshot.child("teacher").getValue(String::class.java) ?: ""

                    if (isCourseAvailable) {
                        if (userRole == "Администратор") {
                            if (teacherId.isNotEmpty()) {
                                getTeacherName(courseId, name, teacherId, isCourseAvailable)
                            } else {
                                val course = Course(
                                    id = courseId,
                                    name = name,
                                    teacher = "Преподаватель не назначен",
                                    description = courseSnapshot.child("description").getValue(String::class.java) ?: "Описание курса $name",
                                    isAvailable = isCourseAvailable
                                )
                                coursesList.add(course)
                            }
                        }
                        else if (userRole == "Преподаватель") {
                            if (teacherId == currentUserId) {
                                if (teacherId.isNotEmpty()) {
                                    getTeacherName(courseId, name, teacherId, isCourseAvailable)
                                } else {
                                    val course = Course(
                                        id = courseId,
                                        name = name,
                                        teacher = "Преподаватель не назначен",
                                        description = courseSnapshot.child("description").getValue(String::class.java) ?: "Описание курса $name",
                                        isAvailable = isCourseAvailable
                                    )
                                    coursesList.add(course)
                                }
                            }
                        }
                        else {
                            if (teacherId.isNotEmpty()) {
                                getTeacherName(courseId, name, teacherId, isCourseAvailable)
                            } else {
                                val course = Course(
                                    id = courseId,
                                    name = name,
                                    teacher = "Преподаватель не назначен",
                                    description = courseSnapshot.child("description").getValue(String::class.java) ?: "Описание курса $name",
                                    isAvailable = isCourseAvailable
                                )
                                coursesList.add(course)
                            }
                        }
                    }
                }

                if (snapshot.childrenCount.toInt() == coursesList.size) {
                    showLoading(false)
                }

                adapter.notifyDataSetChanged()

                if (coursesList.isEmpty()) {
                    Log.d("CoursesActivity", "Не найдено курсов для отображения")
                    showLoading(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CoursesActivity", "Ошибка загрузки курсов: ${error.message}")
                showLoading(false)
                Toast.makeText(this@CoursesActivity, "Ошибка загрузки курсов", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getTeacherName(courseId: String, courseName: String, teacherId: String, isAvailable: Boolean) {
        val database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference
        database.child("users").child(teacherId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val teacherName = snapshot.child("name").getValue(String::class.java) ?: ""
                val course = Course(
                    id = courseId,
                    name = courseName,
                    teacher = teacherName,
                    description = snapshot.child("description").getValue(String::class.java) ?: "Описание курса $courseName",
                    isAvailable = isAvailable
                )
                coursesList.add(course)
                adapter.notifyDataSetChanged()

                if (coursesList.isNotEmpty()) {
                    showLoading(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CoursesActivity", "Ошибка получения данных преподавателя: ${error.message}")

                val course = Course(
                    id = courseId,
                    name = courseName,
                    teacher = "Преподаватель",
                    description = "Описание курса $courseName",
                    isAvailable = isAvailable
                )
                coursesList.add(course)
                adapter.notifyDataSetChanged()

                if (coursesList.isNotEmpty()) {
                    showLoading(false)
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressIndicator.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            progressIndicator.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    class CoursesAdapter(
        private val courses: List<Course>,
        private val onCourseClick: (String) -> Unit
    ) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return CourseViewHolder(view)
        }

        override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
            val course = courses[position]
            holder.bind(course)
        }

        override fun getItemCount(): Int = courses.size

        inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.courseNameText)
            private val teacherText: TextView = itemView.findViewById(R.id.courseTeacherText)
            private val descriptionText: TextView = itemView.findViewById(R.id.courseDescriptionText)
            private val statusText: TextView = itemView.findViewById(R.id.courseStatusText)
            private val actionButton: MaterialButton = itemView.findViewById(R.id.courseActionButton)

            fun bind(course: Course) {
                nameText.text = course.name
                teacherText.text = course.teacher
                descriptionText.text = course.description
                statusText.text = "В процессе"
                actionButton.text = "Посещаемость"
                actionButton.isEnabled = true
                actionButton.setOnClickListener {
                    onCourseClick(course.id)
                }
            }
        }
    }

    data class Course(
        val id: String,
        val name: String,
        val teacher: String,
        val description: String,
        val isAvailable: Boolean
    )
}