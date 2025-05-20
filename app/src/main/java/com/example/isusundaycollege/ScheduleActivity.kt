package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.isusundaycollege.databinding.ActivityScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var lessonsRecyclerView: RecyclerView
    private lateinit var adapter: LessonsAdapter
    private lateinit var database: DatabaseReference
    private var isTeacher = false
    private var currentUserId = ""
    private val lessonsList = mutableListOf<Lesson>()
    private lateinit var binding: ActivityScheduleBinding
    private val weekDays = mapOf(
        Calendar.MONDAY to "Понедельник",
        Calendar.TUESDAY to "Вторник",
        Calendar.WEDNESDAY to "Среда",
        Calendar.THURSDAY to "Четверг",
        Calendar.FRIDAY to "Пятница",
        Calendar.SATURDAY to "Суббота",
        Calendar.SUNDAY to "Воскресенье"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        calendarView = binding.calendarView
        selectedDateText = binding.selectedDateText
        lessonsRecyclerView = binding.lessonsRecyclerView

        lessonsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LessonsAdapter(lessonsList)
        lessonsRecyclerView.adapter = adapter

        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
        selectedDateText.text = "Занятия на ${dateFormat.format(currentDate.time)}"

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)

            val formattedDate = dateFormat.format(selectedDate.time)
            selectedDateText.text = "Занятия на $formattedDate"

            loadLessonsForDate(selectedDate)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainScreen()
            }
        })

        checkTeacherRole()
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this@ScheduleActivity, testActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun checkTeacherRole() {
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (currentUserId.isEmpty()) {
            val currentDate = Calendar.getInstance()
            loadLessonsForDate(currentDate)
            return
        }

        database.child("users").child(currentUserId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isTeacher = snapshot.exists() && snapshot.getValue(String::class.java) == "Преподаватель"
                    val currentDate = Calendar.getInstance()
                    loadLessonsForDate(currentDate)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScheduleActivity", "Ошибка проверки роли пользователя: ${error.message}")
                    val currentDate = Calendar.getInstance()
                    loadLessonsForDate(currentDate)
                }
            })
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.schedule_title)
        binding.toolbar.setNavigationOnClickListener {
            navigateToMainScreen()
        }
    }

    private fun loadLessonsForDate(date: Calendar) {
        lessonsList.clear()

        val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
        val ruDayOfWeek = weekDays[dayOfWeek] ?: ""

        if (isTeacher && currentUserId.isNotEmpty()) {
            loadTeacherLessons(ruDayOfWeek)
        } else {
            loadAllLessons(ruDayOfWeek)
        }
    }

    private fun loadTeacherLessons(dayOfWeek: String) {
        database.child("groups")
            .orderByChild("teacher")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (groupSnapshot in snapshot.children) {
                        val groupWeekDay = groupSnapshot.child("weekDay").getValue(String::class.java)
                        if (groupWeekDay == dayOfWeek) {
                            processGroupData(groupSnapshot)
                        }
                    }

                    if (lessonsList.isEmpty()) {
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScheduleActivity", "Ошибка загрузки занятий преподавателя: ${error.message}")
                }
            })
    }

    private fun loadAllLessons(dayOfWeek: String) {
        database.child("groups")
            .orderByChild("weekDay")
            .equalTo(dayOfWeek)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        adapter.notifyDataSetChanged()
                        return
                    }

                    for (groupSnapshot in snapshot.children) {
                        processGroupData(groupSnapshot)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScheduleActivity", "Ошибка загрузки занятий: ${error.message}")
                }
            })
    }

    private fun processGroupData(groupSnapshot: DataSnapshot) {
        val groupId = groupSnapshot.key ?: return
        val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: "Без названия"
        val time = groupSnapshot.child("time").getValue(String::class.java) ?: "Не указано"
        val courseId = groupSnapshot.child("course").getValue(String::class.java)
        val teacherId = groupSnapshot.child("teacher").getValue(String::class.java)

        if (courseId != null && courseId.isNotEmpty()) {
            database.child("courses").child(courseId).get().addOnSuccessListener { courseSnapshot ->
                val courseName = courseSnapshot.child("name").getValue(String::class.java) ?: "Без названия"

                getTeacherInfo(teacherId) { teacherName ->
                    val timeRange = time.split(" - ")
                    val startTime = if (timeRange.isNotEmpty()) timeRange[0] else "00:00"
                    val lesson = Lesson(
                        id = groupId,
                        groupName = groupName,
                        courseName = courseName,
                        teacherName = teacherName,
                        time = time,
                        startTime = startTime
                    )
                    addLessonToList(lesson)
                }
            }.addOnFailureListener { e ->
                Log.e("ScheduleActivity", "Ошибка загрузки курса: ${e.message}")
                addDefaultLesson(groupId, groupName, time, teacherId)
            }
        } else {
            addDefaultLesson(groupId, groupName, time, teacherId)
        }
    }

    private fun addDefaultLesson(groupId: String, groupName: String, time: String, teacherId: String?) {
        getTeacherInfo(teacherId) { teacherName ->
            val timeRange = time.split(" - ")
            val startTime = if (timeRange.isNotEmpty()) timeRange[0] else "00:00"

            val lesson = Lesson(
                id = groupId,
                groupName = groupName,
                courseName = "Без курса",
                teacherName = teacherName,
                time = time,
                startTime = startTime
            )
            addLessonToList(lesson)
        }
    }

    private fun addLessonToList(lesson: Lesson) {
        lessonsList.add(lesson)
        lessonsList.sortBy { it.startTime }
        adapter.notifyDataSetChanged()
    }

    private fun getTeacherInfo(teacherId: String?, callback: (String) -> Unit) {
        if (teacherId != null && teacherId.isNotEmpty() && teacherId != "none") {
            database.child("users").child(teacherId).get().addOnSuccessListener { teacherSnapshot ->
                val teacherName = teacherSnapshot.child("name").getValue(String::class.java) ?: "Без преподавателя"
                callback(teacherName)
            }.addOnFailureListener {
                callback("Без преподавателя")
            }
        } else {
            callback("Без преподавателя")
        }
    }
}

data class Lesson(
    val id: String,
    val groupName: String,
    val courseName: String,
    val teacherName: String,
    val time: String,
    val startTime: String
)

class LessonsAdapter(private val lessons: List<Lesson>) : RecyclerView.Adapter<LessonsAdapter.LessonViewHolder>() {

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLessonTime: TextView = view.findViewById(R.id.tvLessonTime)
        val tvLessonName: TextView = view.findViewById(R.id.tvLessonName)
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvTeacherName: TextView = view.findViewById(R.id.tvTeacherName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        holder.tvLessonTime.text = lesson.time
        holder.tvLessonName.text = lesson.courseName
        holder.tvGroupName.text = "Группа: ${lesson.groupName}"
        holder.tvTeacherName.text = "Преподаватель: ${lesson.teacherName}"
    }

    override fun getItemCount() = lessons.size
}