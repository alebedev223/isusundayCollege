package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class CoursesActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses)

        FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("CoursesActivity", "Firebase подключение: ${if (connected) "активно" else "отсутствует"}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CoursesActivity", "Ошибка проверки подключения: ${error.message}")
            }
        })

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val pagerAdapter = CoursesPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.available_courses)
                1 -> getString(R.string.unavailable_courses)
                else -> null
            }
        }.attach()

    }

    private inner class CoursesPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CoursesListFragment(true)
                1 -> CoursesListFragment(false)
                else -> CoursesListFragment(true)
            }
        }
    }
}

class CoursesListFragment(val isAvailable: Boolean) : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val coursesList = mutableListOf<Course>()
    private lateinit var adapter: CoursesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_courses_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.coursesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = CoursesAdapter(coursesList) { courseId ->
            val intent = Intent(requireActivity(), AttendanceActivity::class.java)
            intent.putExtra("COURSE_ID", courseId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        loadCourses()
    }

    private fun loadCourses() {
        Log.d("CoursesFragment", "Начало загрузки курсов. isAvailable=$isAvailable")
        val fbDb = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/")
        val database = fbDb.reference
        database.child("courses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("CoursesFragment", "onDataChange вызван")

                if (!snapshot.exists()) {
                    Log.d("CoursesFragment", "Данные о курсах отсутствуют")
                    return
                }

                Log.d("CoursesFragment", "Количество курсов: ${snapshot.childrenCount}")

                coursesList.clear()

                for (courseSnapshot in snapshot.children) {
                    val courseId = courseSnapshot.key ?: continue
                    val name = courseSnapshot.child("name").getValue(String::class.java) ?: "Без названия"

                    Log.d("CoursesFragment", "Обрабатываем курс: $courseId - $name")

                    val course = Course(
                        id = courseId,
                        name = name,
                        teacher = "Преподаватель",
                        description = "Описание курса $name",
                        isAvailable = isAvailable
                    )

                    coursesList.add(course)
                }

                Log.d("CoursesFragment", "Добавлено курсов: ${coursesList.size}")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CoursesFragment", "Ошибка загрузки курсов: ${error.message}")
            }
        })
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
            statusText.text = if (course.isAvailable) "В процессе" else "Недоступно"

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
)}