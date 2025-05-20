package com.example.isusundaycollege

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.isusundaycollege.databinding.ActivityGroupsBinding
import com.example.isusundaycollege.databinding.ActivityProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*

class GroupActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupsAdapter
    private val groupsList = mutableListOf<Group>()
    private lateinit var database: DatabaseReference
    private val coursesList = mutableListOf<Course>()
    private val teachersList = mutableListOf<Teacher>()
    private lateinit var binding: ActivityGroupsBinding
    private val timeArray = arrayOf(
        "8:30 - 10:00",
        "10:10 - 11:40",
        "11:50 - 13:20",
        "13:50 - 15:20",
        "15:30 - 17:00"
    )
    private val weekArray = arrayOf(
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота",
        "Воскресенье"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance("https://isu-sundaycollege-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Управление группами"

        recyclerView = findViewById(R.id.recyclerGroups)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GroupsAdapter(
            groupsList,
            onEditClick = { group -> showEditGroupDialog(group) },
            onDeleteClick = { group -> showDeleteGroupDialog(group) },
            onManageStudentsClick = { group -> showManageStudentsDialog(group) }
        )
        recyclerView.adapter = adapter
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@GroupActivity, testActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        })
        loadTeachers()
        loadCourses()
        loadGroups()
        setupToolbar()
        val fab = findViewById<FloatingActionButton>(R.id.fabAddGroup)
        fab.setOnClickListener {
            showAddGroupDialog()
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Управление группами"
        binding.toolbar.setNavigationOnClickListener {
            val intent = Intent(this@GroupActivity, testActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
    private fun loadTeachers(){
        teachersList.add(Teacher("none", "Без преподавателя"))
        database.child("users").addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children){
                    val userid=snap.key?:continue
                    val role=snap.child("role").getValue(String::class.java)
                    if (role=="Преподаватель"){
                        val teacherName=snap.child("name").getValue(String::class.java)?:"Преподаватель без имени"
                        teachersList.add(Teacher(userid,teacherName))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupActivity", "Ошибка загрузки преподавателей: ${error.message}")
                Toast.makeText(this@GroupActivity, "Ошибка загрузки преподавателей", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun loadCourses() {
        database.child("courses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coursesList.clear()

                coursesList.add(Course("none", "Без курса", null))

                for (courseSnapshot in snapshot.children) {
                    val courseId = courseSnapshot.key ?: continue
                    val courseName = courseSnapshot.child("name").getValue(String::class.java) ?: "Без названия"
                    val teacherId = courseSnapshot.child("teacher").getValue(String::class.java)

                    coursesList.add(Course(courseId, courseName, teacherId))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupActivity", "Ошибка загрузки курсов: ${error.message}")
                Toast.makeText(this@GroupActivity, "Ошибка загрузки курсов", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadGroups() {
        database.child("groups").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupsList.clear()

                for (groupSnapshot in snapshot.children) {
                    val groupId = groupSnapshot.key ?: continue
                    val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: "Без названия"
                    val courseId = groupSnapshot.child("course").getValue(String::class.java)
                    val teacherId = groupSnapshot.child("teacher").getValue(String::class.java)
                    val weekDay = groupSnapshot.child("weekDay").getValue(String::class.java) ?: "Не указан"
                    val time = groupSnapshot.child("time").getValue(String::class.java) ?: "Не указано"
                    val course = if (courseId != null) {
                        coursesList.find { it.id == courseId }
                    } else null
                    val teacher = if (teacherId != null && teacherId.isNotEmpty()) {
                        teachersList.find { it.id == teacherId }
                    } else null

                    val teacherName = teacher?.name ?: "Без преподавателя"
                    val courseName = course?.name ?: "Без курса"

                    val studentsList = mutableListOf<Student>()

                    val studentsSnapshot = groupSnapshot.child("students")
                    for (studentEntry in studentsSnapshot.children) {
                        val studentId = studentEntry.key ?: continue
                        val isInGroup = studentEntry.getValue(Boolean::class.java) ?: false
                        if (isInGroup) {
                            database.child("students").child(studentId).get().addOnSuccessListener { studentData ->
                                val studentName = studentData.child("name").getValue(String::class.java) ?: "Неизвестный студент"

                                val groupIndex = groupsList.indexOfFirst { it.id == groupId }
                                if (groupIndex != -1) {
                                    val currentGroup = groupsList[groupIndex]
                                    val student = Student(studentId, studentName, currentGroup)
                                    val updatedStudents = currentGroup.students.toMutableList()
                                    updatedStudents.add(student)
                                    groupsList[groupIndex] = currentGroup.copy(students = updatedStudents)
                                    adapter.notifyItemChanged(groupIndex)
                                }
                            }
                        }
                    }
                    val group = Group(
                        id = groupId,
                        name = groupName,
                        courseId = courseId,
                        courseName = courseName,
                        students = studentsList,
                        teacherId = teacherId,
                        teacherName = teacherName,
                        weekDay = weekDay,
                        time = time
                    )
                    groupsList.add(group)
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupActivity", "Ошибка загрузки групп: ${error.message}")
                Toast.makeText(this@GroupActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddGroupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_group, null)
        val editTextGroupName = dialogView.findViewById<TextInputEditText>(R.id.editTextGroupName)
        val courseDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.courseDropdown)
        val teacherDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.teacherDropdown)
        val weekDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.weekDropdown)
        val timeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.timeDropdown)
        setupCourseDropdown(courseDropdown)
        setupTeacherDropdown(teacherDropdown)
        setupWeekDropdown(weekDropdown)
        setupTimeDropdown(timeDropdown)
        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить группу")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val groupName = editTextGroupName.text.toString()
                val selectedCourse = courseDropdown.text.toString()
                val courseId = coursesList.find { it.name == selectedCourse }?.id
                val selectedTeacher = teacherDropdown.text.toString()
                val teacherId = teachersList.find { it.name == selectedTeacher }?.id
                val selectedDay = weekDropdown.text.toString()
                val selectedTime = timeDropdown.text.toString()
                if (groupName.isNotEmpty()) {
                    addGroup(groupName, courseId, teacherId, selectedDay, selectedTime)
                } else {
                    Toast.makeText(this, "Имя группы не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupCourseDropdown(courseDropdown: AutoCompleteTextView) {
        val courseNames = coursesList.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, courseNames)
        courseDropdown.setAdapter(adapter)

        if (courseNames.isNotEmpty()) {
            courseDropdown.setText(courseNames[0], false)
        }
    }private fun setupTeacherDropdown(teacherDropdown: AutoCompleteTextView) {
        val teacherNames = teachersList.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, teacherNames)
        teacherDropdown.setAdapter(adapter)

        if (teacherNames.isNotEmpty()) {
            teacherDropdown.setText(teacherNames[0], false)
        }
    }
    private fun setupWeekDropdown(weekDropdown: AutoCompleteTextView) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, weekArray)
        weekDropdown.setAdapter(adapter)
        weekDropdown.setText(weekArray[6], false)
    }

    private fun setupTimeDropdown(timeDropdown: AutoCompleteTextView) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, timeArray)
        timeDropdown.setAdapter(adapter)
        if (timeArray.isNotEmpty()) {
            timeDropdown.setText(timeArray[0], false)
        }
    }

    private fun addGroup(groupName: String, courseId: String?,teacherId: String?, weekDay: String, time: String) {

        val groupId = database.child("groups").push().key ?: return
        val groupData = HashMap<String, Any?>()

        groupData["name"] = groupName
        groupData["weekDay"] = weekDay
        groupData["time"] = time

        if (courseId != null && courseId != "none") {
            groupData["course"] = courseId
        }
        if (teacherId != null && teacherId != "none") {
            groupData["teacher"] = teacherId
        }
        database.child("groups").child(groupId).setValue(groupData)
            .addOnSuccessListener {
                Toast.makeText(this, "Группа добавлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GroupActivity", "Ошибка добавления группы: ${e.message}")
                Toast.makeText(this, "Ошибка добавления группы", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditGroupDialog(group: Group) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_group, null)
        val editTextGroupName = dialogView.findViewById<TextInputEditText>(R.id.editTextGroupName)
        val courseDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.courseDropdown)
        val teacherDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.teacherDropdown)
        val weekDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.weekDropdown)
        val timeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.timeDropdown)
        setupCourseDropdown(courseDropdown)
        setupTeacherDropdown(teacherDropdown)
        setupWeekDropdown(weekDropdown)
        setupTimeDropdown(timeDropdown)
        editTextGroupName.setText(group.name)
        if (group.teacherName.isNotEmpty() && group.teacherName != "Без преподавателя") {
            teacherDropdown.setText(group.teacherName, false)
        } else {
            teacherDropdown.setText("Без преподавателя", false)
        }
        courseDropdown.setText(group.courseName, false)
        teacherDropdown.setText(group.teacherName, false)
        weekDropdown.setText(group.weekDay, false)
        timeDropdown.setText(group.time, false)
        MaterialAlertDialogBuilder(this)
            .setTitle("Редактировать группу")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val groupName = editTextGroupName.text.toString()
                val selectedCourse = courseDropdown.text.toString()
                val courseId = coursesList.find { it.name == selectedCourse }?.id
                val selectedTeacher = teacherDropdown.text.toString()
                val teacherId = teachersList.find { it.name == selectedTeacher }?.id
                val selectedDay = weekDropdown.text.toString()
                val selectedTime = timeDropdown.text.toString()
                if (groupName.isNotEmpty()) {
                    updateGroup(group.id, groupName, courseId, teacherId, selectedDay, selectedTime)
                } else {
                    Toast.makeText(this, "Имя группы не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateGroup(groupId: String, groupName: String, courseId: String?, teacherId: String?, weekDay: String, time: String) {
        val updates = HashMap<String, Any?>()
        updates["groups/$groupId/name"] = groupName
        updates["groups/$groupId/weekDay"] = weekDay
        updates["groups/$groupId/time"] = time
        if (courseId != null && courseId != "none") {
            updates["groups/$groupId/course"] = courseId
        } else {
            updates["groups/$groupId/course"] = null
        }
        if (teacherId != null && teacherId != "none") {
            updates["groups/$groupId/teacher"] = teacherId
        } else {
            updates["groups/$groupId/teacher"] = null
        }

        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Группа обновлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GroupActivity", "Ошибка обновления группы: ${e.message}")
                Toast.makeText(this, "Ошибка обновления группы", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteGroupDialog(group: Group) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Удалить группу")
            .setMessage("Вы уверены, что хотите удалить группу \"${group.name}\"? Это также удалит связи студентов с этой группой.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteGroup(group.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteGroup(groupId: String) {
        database.child("groups").child(groupId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Группа удалена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GroupActivity", "Ошибка удаления группы: ${e.message}")
                Toast.makeText(this, "Ошибка удаления группы", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showManageStudentsDialog(group: Group) {
        database.child("students").get().addOnSuccessListener { snapshot ->
            val allStudents = mutableListOf<Student>()

            for (studentSnapshot in snapshot.children) {
                val studentId = studentSnapshot.key ?: continue
                val studentName = studentSnapshot.child("name").getValue(String::class.java) ?: "Неизвестный студент"
                allStudents.add(Student(studentId, studentName, null))
            }
            if (allStudents.isEmpty()) {
                Toast.makeText(this, "Нет доступных студентов", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val currentStudentsIds = group.students.map { it.id }
            val checkedItems = allStudents.map { currentStudentsIds.contains(it.id) }.toBooleanArray()
            val studentNames = allStudents.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Управление студентами группы: ${group.name}")
                .setMultiChoiceItems(studentNames, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("Сохранить") { _, _ ->
                    val updates = HashMap<String, Any?>()
                    for (student in group.students) {
                        updates["groups/${group.id}/students/${student.id}"] = null
                    }
                    for (i in checkedItems.indices) {
                        if (checkedItems[i]) {
                            val studentId = allStudents[i].id
                            updates["groups/${group.id}/students/$studentId"] = true
                        }
                    }
                    database.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Список студентов обновлен", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("GroupActivity", "Ошибка обновления списка студентов: ${e.message}")
                            Toast.makeText(this, "Ошибка обновления списка студентов", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }.addOnFailureListener { e ->
            Log.e("GroupActivity", "Ошибка загрузки студентов: ${e.message}")
            Toast.makeText(this, "Ошибка загрузки студентов", Toast.LENGTH_SHORT).show()
        }
    }

    fun goToStudentManagement(view: View) {
        startActivity(Intent(this, StudentAddActivity::class.java))
    }
}

class GroupsAdapter(
    private val groups: List<Group>,
    private val onEditClick: (Group) -> Unit,
    private val onDeleteClick: (Group) -> Unit,
    private val onManageStudentsClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvStudentsCount: TextView = view.findViewById(R.id.tvStudentsCount)
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val tvTeacherName: TextView = view.findViewById(R.id.tvTeacherName)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnEdit: View = view.findViewById(R.id.btnEditGroup)
        val btnDelete: View = view.findViewById(R.id.btnDeleteGroup)
        val btnManageStudents: View = view.findViewById(R.id.btnManageStudents)
    }
    fun getStudentCount(num: Int): String {
        return when {
            num % 100 in 11..19 -> "$num студентов"
            num % 10 == 1 -> "$num студент"
            num % 10 in 2..4 -> "$num студента"
            else -> "$num студентов"
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]

        holder.tvGroupName.text = group.name
        holder.tvStudentsCount.text = getStudentCount(group.students.size)
        holder.tvCourseName.text = group.courseName
        holder.tvTeacherName.text = "Преподаватель: ${group.teacherName}"
        holder.tvTime.text = "Время занятий: ${group.weekDay}, ${group.time}"

        holder.btnEdit.setOnClickListener { onEditClick(group) }
        holder.btnDelete.setOnClickListener { onDeleteClick(group) }
        holder.btnManageStudents.setOnClickListener { onManageStudentsClick(group) }
    }

    override fun getItemCount() = groups.size
}

data class Group(
    val id: String,
    val name: String,
    val courseId: String? = null,
    val courseName: String = "Без курса",
    val students: List<Student> = emptyList(),
    val teacherId: String? = null,
    val teacherName: String = "Без преподавателя",
    val weekDay: String = "Не указан",
    val time: String = "Не указано"
)

data class Student(
    val id: String,
    val name: String,
    val group: Group? = null
)

data class Course(
    val id: String,
    val name: String,
    val teacherId: String? = null
)

data class Teacher(
    val id: String,
    val name: String
)