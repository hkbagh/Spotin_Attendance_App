package com.hkbagh.spotin

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.util.TypedValue

class AllEmployeesAttendanceActivity : AppCompatActivity() {

    private lateinit var monthYearTextView: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var daysHeaderLayout: LinearLayout
    private lateinit var employeesAttendanceRecyclerView: RecyclerView
    private lateinit var adapter: EmployeeAttendanceSummaryAdapter

    private lateinit var appwriteClient: Client
    private lateinit var databases: Databases

    private var currentCalendar: Calendar = Calendar.getInstance()
    private var employeeProfilesMap = mutableMapOf<String, String>() // Map to store employee_id to name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_employees_attendance)

        monthYearTextView = findViewById(R.id.monthYearTextView)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)
        daysHeaderLayout = findViewById(R.id.daysHeaderLayout)
        employeesAttendanceRecyclerView = findViewById(R.id.employeesAttendanceRecyclerView)

        appwriteClient = Client(applicationContext)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("684c2359000d2bf66544") // Your Appwrite project ID
            .setSelfSigned(true)

        databases = Databases(appwriteClient)

        adapter = EmployeeAttendanceSummaryAdapter(this, mutableListOf())
        employeesAttendanceRecyclerView.layoutManager = LinearLayoutManager(this)
        employeesAttendanceRecyclerView.adapter = adapter

        prevMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthYearDisplay()
            CoroutineScope(Dispatchers.Main).launch {
                fetchAllEmployeeAttendance()
            }
        }

        nextMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthYearDisplay()
            CoroutineScope(Dispatchers.Main).launch {
                fetchAllEmployeeAttendance()
            }
        }

        updateMonthYearDisplay()
        CoroutineScope(Dispatchers.Main).launch {
            fetchAllEmployeeAttendance()
        }
    }

    private fun updateMonthYearDisplay() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTextView.text = dateFormat.format(currentCalendar.time)
        updateDaysHeader()
    }

    private fun updateDaysHeader() {
        daysHeaderLayout.removeAllViews()
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..daysInMonth) {
            val dayTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.day_column_width), // Define this dimen in dimens.xml
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(4, 0, 4, 0)
                text = i.toString()
            }
            daysHeaderLayout.addView(dayTextView)
        }
    }

    private suspend fun fetchAllEmployeeAttendance() {
        withContext(Dispatchers.IO) {
            try {
                // Fetch user profiles first to get names
                fetchUserProfiles()

                val databaseId = "684d43c6001eb3b4547b" // Your attendance database ID
                val collectionId = "684d43e5000fb8fc2d56" // Your attendance collection ID

                val startOfMonth = currentCalendar.clone() as Calendar
                startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
                startOfMonth.set(Calendar.HOUR_OF_DAY, 0)
                startOfMonth.set(Calendar.MINUTE, 0)
                startOfMonth.set(Calendar.SECOND, 0)
                startOfMonth.set(Calendar.MILLISECOND, 0)

                val endOfMonth = currentCalendar.clone() as Calendar
                endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
                endOfMonth.set(Calendar.HOUR_OF_DAY, 23)
                endOfMonth.set(Calendar.MINUTE, 59)
                endOfMonth.set(Calendar.SECOND, 59)
                endOfMonth.set(Calendar.MILLISECOND, 999)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

                val attendanceResponse = databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = listOf(
                        Query.greaterThanEqual("timestamp", dateFormat.format(startOfMonth.time)),
                        Query.lessThanEqual("timestamp", dateFormat.format(endOfMonth.time)),
                        Query.orderAsc("employee_id"),
                        Query.orderAsc("timestamp")
                    )
                )

                val employeeAttendanceData = mutableMapOf<String, MutableMap<Int, String>>() // employee_id -> day -> status
                val employeeTotalPresent = mutableMapOf<String, Int>()
                val employeeTotalAbsent = mutableMapOf<String, Int>()

                val distinctEmployeeIds = attendanceResponse.documents.mapNotNull { it.data["employee_id"] as? String }.distinct()
                val distinctAttendanceDaysInMonth = mutableSetOf<Int>() // To store only days with attendance data

                val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                // Initialize all days for all distinct employee IDs found in attendance as Absent by default
                distinctEmployeeIds.forEach { employeeId ->
                    employeeAttendanceData[employeeId] = (1..daysInMonth).associateWith { "A" }.toMutableMap()
                    employeeTotalPresent[employeeId] = 0
                    employeeTotalAbsent[employeeId] = daysInMonth // All absent by default
                }

                for (document in attendanceResponse.documents) {
                    val employeeId = document.data["employee_id"] as? String
                    val status = document.data["status"] as? String
                    val timestampString = document.data["timestamp"] as? String

                    if (employeeId != null && status != null && timestampString != null) {
                        try {
                            val attendanceDate = dateFormat.parse(timestampString)
                            if (attendanceDate != null) {
                                val cal = Calendar.getInstance().apply { time = attendanceDate }
                                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                                distinctAttendanceDaysInMonth.add(dayOfMonth) // Add day to the set

                                val currentEmployeeDailyData = employeeAttendanceData.getOrPut(employeeId) { (1..daysInMonth).associateWith { "A" }.toMutableMap() }
                                currentEmployeeDailyData[dayOfMonth] = status

                                if (status == "P") {
                                    employeeTotalPresent[employeeId] = (employeeTotalPresent[employeeId] ?: 0) + 1
                                    employeeTotalAbsent[employeeId] = (employeeTotalAbsent[employeeId] ?: daysInMonth) - 1 // Decrement absent count
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AllEmployeesAttendance", "Error parsing date or processing attendance: ${e.message}", e)
                        }
                    }
                }

                // Construct summary list from distinct employee IDs and their processed attendance
                val summaryList = distinctEmployeeIds.map { employeeId ->
                    EmployeeAttendanceSummary(
                        employeeId = employeeId,
                        employeeName = employeeProfilesMap[employeeId] ?: employeeId,
                        dailyStatus = employeeAttendanceData[employeeId] ?: emptyMap(),
                        totalPresent = employeeTotalPresent[employeeId] ?: 0,
                        totalAbsent = employeeTotalAbsent[employeeId] ?: daysInMonth
                    )
                }.sortedBy { it.employeeName }

                withContext(Dispatchers.Main) {
                    // Pass distinct attendance days to updateDaysHeader and adapter
                    updateDaysHeader(distinctAttendanceDaysInMonth.sorted())
                    adapter.updateData(summaryList, distinctAttendanceDaysInMonth.sorted())
                    Toast.makeText(this@AllEmployeesAttendanceActivity, "Attendance data loaded.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AllEmployeesAttendance", "Failed to fetch all employee attendance: ${e.message}", e)
                    Toast.makeText(this@AllEmployeesAttendanceActivity, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun fetchUserProfiles() {
        try {
            val response = databases.listDocuments(
                databaseId = Appwrite.APPWRITE_DATABASE_ID,
                collectionId = Appwrite.APPWRITE_COLLECTION_ID,
                queries = listOf(Query.limit(100)) // Fetch all profiles, adjust limit if you have more than 100
            )
            employeeProfilesMap.clear()
            for (document in response.documents) {
                val userId = document.data["user_id"] as? String
                val employeeId = document.data["employee_id"] as? String
                val name = document.data["name"] as? String
                if (employeeId != null && name != null) {
                    employeeProfilesMap[employeeId] = name
                }
            }
            Log.d("AllEmployeesAttendance", "Fetched ${employeeProfilesMap.size} user profiles.")
        } catch (e: Exception) {
            Log.e("AllEmployeesAttendance", "Error fetching user profiles: ${e.message}", e)
        }
    }

    private fun updateDaysHeader(distinctDays: List<Int>) {
        daysHeaderLayout.removeAllViews()
        // Add an empty TextView for the Name column placeholder
        val namePlaceholder = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.name_column_width), // Define this dimen in dimens.xml
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Name"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(4, 0, 4, 0)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        daysHeaderLayout.addView(namePlaceholder)

        for (i in distinctDays) {
            val dayTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.day_column_width), // Define this dimen in dimens.xml
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(4, 0, 4, 0)
                text = i.toString()
            }
            daysHeaderLayout.addView(dayTextView)
        }

        // Add headers for Total Present and Total Absent
        val totalPresentHeader = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(8, 0, 8, 0)
            text = "P"
        }
        daysHeaderLayout.addView(totalPresentHeader)

        val totalAbsentHeader = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(8, 0, 8, 0)
            text = "A"
        }
        daysHeaderLayout.addView(totalAbsentHeader)
    }
}
