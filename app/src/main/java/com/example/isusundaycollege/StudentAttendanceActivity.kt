package com.example.isusundaycollege

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvCourseName: TextView
    private lateinit var tvGroupName: TextView
    private lateinit var tvAttendanceRate: TextView
    private lateinit var recyclerAttendanceHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var database: DatabaseReference
    private lateinit var adapter: AttendanceHistoryAdapter

    private var studentId: String? = null
    private var courseId: String? = null
    private var groupId: String? = null
    private val attendanceHistoryList = mutableListOf<AttendanceRecord>()

    private val dateFormatDisplay = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val dateFormatDatabase = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_attendance)

        val fbDb = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        database = fbDb.reference

        studentId = intent.getStringExtra("STUDENT_ID")
        courseId = intent.getStringExtra("COURSE_ID")
        groupId = intent.getStringExtra("GROUP_ID")

        if (studentId == null || courseId == null || groupId == null) {
            Toast.makeText(this, "Отсутствуют необходимые данные", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        loadStudentInfo()
        loadAttendanceHistory()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvCourseName = findViewById(R.id.tvCourseName)
        tvGroupName = findViewById(R.id.tvGroupName)
        tvAttendanceRate = findViewById(R.id.tvAttendanceRate)
        recyclerAttendanceHistory = findViewById(R.id.recyclerAttendanceHistory)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Профиль посещаемости"
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = AttendanceHistoryAdapter(attendanceHistoryList)
        recyclerAttendanceHistory.layoutManager = LinearLayoutManager(this)
        recyclerAttendanceHistory.adapter = adapter
    }

    private fun loadStudentInfo() {
        database.child("students").child(studentId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Неизвестный студент"
                    tvStudentName.text = name
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentAttendanceProfile", "Ошибка загрузки данных студента: ${error.message}")
                }
            })
        
        database.child("courses").child(courseId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val courseName = snapshot.child("name").getValue(String::class.java) ?: "Неизвестный курс"
                    tvCourseName.text = courseName
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentAttendanceProfile", "Ошибка загрузки данных курса: ${error.message}")
                }
            })

        database.child("groups").child(groupId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupName = snapshot.child("name").getValue(String::class.java) ?: "Неизвестная группа"
                    tvGroupName.text = groupName
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentAttendanceProfile", "Ошибка загрузки данных группы: ${error.message}")
                }
            })
    }

    private fun loadAttendanceHistory() {
        progressBar.visibility = View.VISIBLE

        database.child("attendance").child(courseId!!).child(groupId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    attendanceHistoryList.clear()
                    var presentCount = 0
                    var totalCount = 0

                    for (dateSnapshot in snapshot.children) {
                        val dateStr = dateSnapshot.key ?: continue

                        val studentAttendanceSnapshot = dateSnapshot.child(studentId!!)
                        if (studentAttendanceSnapshot.exists()) {
                            val isPresent = studentAttendanceSnapshot.child("present").getValue(Boolean::class.java) ?: false

                            try {
                                val date = dateFormatDatabase.parse(dateStr)
                                if (date != null) {
                                    val displayDate = dateFormatDisplay.format(date)
                                    attendanceHistoryList.add(AttendanceRecord(displayDate, isPresent))

                                    if (isPresent) {
                                        presentCount++
                                    }
                                    totalCount++
                                }
                            } catch (e: Exception) {
                                Log.e("StudentAttendanceProfile", "Ошибка конвертации даты: ${e.message}")
                            }
                        }
                    }

                    attendanceHistoryList.sortByDescending {
                        try {
                            dateFormatDisplay.parse(it.date)?.time ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    }

                    val attendanceRate = if (totalCount > 0) {
                        (presentCount.toFloat() / totalCount) * 100
                    } else {
                        0f
                    }

                    tvAttendanceRate.text = String.format("%.1f%% (%d из %d)", attendanceRate, presentCount, totalCount)

                    adapter.notifyDataSetChanged()
                    progressBar.visibility = View.GONE

                    if (attendanceHistoryList.isEmpty()) {
                        Toast.makeText(this@StudentAttendanceActivity,
                            "История посещаемости отсутствует",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentAttendanceProfile", "Ошибка загрузки истории посещаемости: ${error.message}")
                    Toast.makeText(this@StudentAttendanceActivity,
                        "Ошибка загрузки истории посещаемости",
                        Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            })
    }

    inner class AttendanceHistoryAdapter(private val records: List<AttendanceRecord>) :
        RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvAttendanceDate: TextView = itemView.findViewById(R.id.tvAttendanceDate)
            val ivAttendanceStatus: ImageView = itemView.findViewById(R.id.ivAttendanceStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_attendance_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = records[position]
            holder.tvAttendanceDate.text = record.date

            if (record.present) {
                holder.ivAttendanceStatus.setImageResource(R.drawable.ic_check)
                holder.ivAttendanceStatus.setColorFilter(resources.getColor(android.R.color.holo_green_dark, theme))
            } else {
                holder.ivAttendanceStatus.setImageResource(R.drawable.ic_close)
                holder.ivAttendanceStatus.setColorFilter(resources.getColor(android.R.color.holo_red_dark, theme))
            }
        }

        override fun getItemCount(): Int = records.size
    }

    data class AttendanceRecord(
        val date: String,
        val present: Boolean
    )
}