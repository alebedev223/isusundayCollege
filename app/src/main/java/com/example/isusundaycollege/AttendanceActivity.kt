package com.example.isusundaycollege

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerGroups: AutoCompleteTextView
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var database: DatabaseReference
    private lateinit var adapter: StudentAttendanceAdapter
    private val studentsList = mutableListOf<StudentAttendance>()

    private var selectedCourse: String? = null
    private var selectedGroup: String? = null
    private var selectedDate: String? = null
    private var dateM: Long = 0

    private val groups = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private var userRole: String = "student"
    private var userId: String = ""
    private var isTeaching: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        val fbDb =
            FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        database = fbDb.reference

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        userId = currentUser.uid

        selectedCourse = intent.getStringExtra("COURSE_ID")
        if (selectedCourse == null) {
            Toast.makeText(this, "ID курса не указан", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        spinnerGroups = findViewById(R.id.spinnerGroups)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        recyclerView = findViewById(R.id.recyclerAttendance)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        checkUserAccess()
    }

    private fun checkUserAccess(){
        database.child("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    userRole=snapshot.getValue(String::class.java)?:"student"
                    when(userRole){
                        "Администратор"->{
                            setupUI()
                        }
                        "Преподаватель"->{
                            checkTeacherAccess()
                        }
                        else->{
                            Toast.makeText(this@AttendanceActivity,"Нет доступа на редактирование",Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AttendanceActivity", "Ошибка проверки роли: ${error.message}")
                    Toast.makeText(this@AttendanceActivity,
                        "Ошибка проверки прав доступа",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun checkTeacherAccess() {
        database.child("courses").child(selectedCourse!!).child("teacher")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val teacherId = snapshot.getValue(String::class.java)
                    isTeaching = teacherId == userId

                    if (isTeaching) {
                        setupUI()
                    } else {
                        Toast.makeText(this@AttendanceActivity,
                            "Вы не являетесь преподавателем этого курса",
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AttendanceActivity", "Ошибка проверки учителя курса: ${error.message}")
                    Toast.makeText(this@AttendanceActivity,
                        "Ошибка проверки прав доступа",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun setupUI(){
        database.child("courses").child(selectedCourse!!).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val courseName = snapshot.getValue(String::class.java)
                    supportActionBar?.title = courseName ?: "Посещаемость"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AttendanceActivity", "Ошибка полуения названия: ${error.message}")
                }
            })

        adapter = StudentAttendanceAdapter(studentsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupGroupsDropdown()
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        btnSave.setOnClickListener {
            saveAttendanceData()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupGroupsDropdown() {
        database.child("groups").orderByChild("course").equalTo(selectedCourse)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groups.clear()
                    val groupNames = mutableListOf<String>()
                    val groupIdName = mutableMapOf<String, String>()

                    for (groupSnapshot in snapshot.children) {
                        val groupId = groupSnapshot.key ?: continue
                        val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: "Группа $groupId"

                        groups.add(groupId)
                        groupNames.add(groupName)
                        groupIdName[groupName] = groupId
                    }

                    val groupsAdapter = ArrayAdapter(
                        this@AttendanceActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        groupNames
                    )
                    spinnerGroups.setAdapter(groupsAdapter)

                    spinnerGroups.setOnItemClickListener { _, _, position, _ ->
                        val selectedGroupName = groupNames[position]
                        selectedGroup = groupIdName[selectedGroupName]
                        if (selectedDate != null) {
                            loadStudentsAttendance()
                        }
                    }

                    if (groupNames.size == 1) {
                        spinnerGroups.setText(groupNames[0], false)
                        selectedGroup = groupIdName[groupNames[0]]
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AttendanceActivity", "Ошибка загрузки групп: ${error.message}")
                    Toast.makeText(this@AttendanceActivity, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                dateM = calendar.timeInMillis
                selectedDate = dateFormat.format(calendar.time)
                tvSelectedDate.text = "Выбранная дата: ${displayDateFormat.format(calendar.time)}"

                if (selectedGroup != null) {
                    loadStudentsAttendance()
                }
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun loadStudentsAttendance() {
        if (selectedCourse == null || selectedGroup == null || selectedDate == null) {
            return
        }

        studentsList.clear()
        database.child("groups").child(selectedGroup!!).child("students")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val studentIds = mutableListOf<String>()
                    for (studentSnapshot in snapshot.children) {
                        val studentId = studentSnapshot.key ?: continue
                        studentIds.add(studentId)
                    }

                    if (studentIds.isEmpty()) {
                        Toast.makeText(this@AttendanceActivity, "В группе нет студентов", Toast.LENGTH_SHORT).show()
                        return
                    }
                    database.child("attendance").child(selectedCourse!!).child(selectedGroup!!)
                        .child(selectedDate!!).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(attendanceSnapshot: DataSnapshot) {
                                var loadedCount = 0
                                for (studentId in studentIds) {
                                    database.child("students").child(studentId)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(studentSnapshot: DataSnapshot) {
                                                val name = studentSnapshot.child("name").getValue(String::class.java) ?: "Неизвестный студент"
                                                val isPresent = attendanceSnapshot.child(studentId)
                                                    .child("present").getValue(Boolean::class.java) ?: false
                                                studentsList.add(StudentAttendance(studentId, name, isPresent))
                                                loadedCount++
                                                if (loadedCount >= studentIds.size) {
                                                    studentsList.sortBy { it.name }
                                                    adapter.notifyDataSetChanged()
                                                }
                                            }
                                            override fun onCancelled(error: DatabaseError) {
                                                loadedCount++
                                                Log.e("AttendanceActivity", "Ошибка загрузки данных студента: ${error.message}")

                                                if (loadedCount >= studentIds.size) {
                                                    adapter.notifyDataSetChanged()
                                                }
                                            }
                                        })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("AttendanceActivity", "Ошибка загрузки посещаемости: ${error.message}")
                                Toast.makeText(
                                    this@AttendanceActivity,
                                    "Ошибка загрузки данных посещаемости",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AttendanceActivity", "Ошибка загрузки данных студентов: ${error.message}")
                    Toast.makeText(this@AttendanceActivity, "Ошибка загрузки списка студентов", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveAttendanceData() {
        if (selectedCourse == null || selectedGroup == null || selectedDate == null) {
            Toast.makeText(this, "Выберите группу и дату", Toast.LENGTH_SHORT).show()
            return
        }

        if (studentsList.isEmpty()) {
            Toast.makeText(this, "Нет данных для сохранения", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = HashMap<String, Any>()

        for (student in studentsList) {
            updates["attendance/$selectedCourse/$selectedGroup/$selectedDate/${student.id}/present"] = student.isPresent
        }
        updates["attendance/$selectedCourse/$selectedGroup/$selectedDate/teacher"] = userId

        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.updateChildren(updates).await()
                runOnUiThread {
                    Toast.makeText(this@AttendanceActivity, "Данные сохранены успешно", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AttendanceActivity", "Ошибка сохранения данных: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Ошибка сохранения данных: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteDialog(student: StudentAttendance) {
        AlertDialog.Builder(this)
            .setTitle("Удалить запись")
            .setMessage("Вы уверены, что хотите удалить запись посещаемости для студента ${student.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteStudentAttendance(student.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteStudentAttendance(studentId: String) {
        if (selectedCourse == null || selectedGroup == null || selectedDate == null) {
            return
        }

        database.child("attendance").child(selectedCourse!!)
            .child(selectedGroup!!).child(selectedDate!!)
            .child(studentId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
                loadStudentsAttendance()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при удалении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class StudentAttendance(
        val id: String,
        val name: String,
        var isPresent: Boolean
    )

    inner class StudentAttendanceAdapter(private val students: List<StudentAttendance>) :
        RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
            val checkboxPresent: MaterialCheckBox = itemView.findViewById(R.id.checkboxPresent)
            val studentCardContainer: View = itemView.findViewById(R.id.studentCardContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_student_attendance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            holder.tvStudentName.text = student.name
            holder.checkboxPresent.isChecked = student.isPresent

            holder.checkboxPresent.setOnCheckedChangeListener { _, isChecked ->
                student.isPresent = isChecked
            }

            holder.itemView.setOnLongClickListener {
                showDeleteDialog(student)
                true
            }
            holder.studentCardContainer.setOnClickListener {
                if (selectedCourse != null && selectedGroup != null) {
                    val intent = Intent(this@AttendanceActivity, StudentAttendanceActivity::class.java).apply {
                        putExtra("STUDENT_ID", student.id)
                        putExtra("COURSE_ID", selectedCourse)
                        putExtra("GROUP_ID", selectedGroup)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Выберите курс и группу сначала",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        override fun getItemCount(): Int = students.size
    }
}