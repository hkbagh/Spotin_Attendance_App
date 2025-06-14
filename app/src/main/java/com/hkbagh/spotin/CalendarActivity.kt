package com.hkbagh.spotin

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import io.appwrite.Query
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Account
import com.applandeo.materialcalendarview.CalendarDay
import android.graphics.drawable.ColorDrawable

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var presentDatesTextView: TextView
    private var allAttendanceDates = mutableMapOf<String, String>()
    private lateinit var appwriteClient: Client
    private lateinit var databases: Databases
    private lateinit var appwriteAccount: Account

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    private val displayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)
        presentDatesTextView = findViewById(R.id.presentDatesTextView)

        appwriteClient = Client(applicationContext)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("684c2359000d2bf66544")
            .setSelfSigned(true)

        databases = Databases(appwriteClient)
        appwriteAccount = Account(appwriteClient)

        // Set listeners for page changes
        calendarView.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                fetchAttendanceDataForMonth()
            }
        })

        calendarView.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                fetchAttendanceDataForMonth()
            }
        })

        // Initial fetch for the current month
        fetchAttendanceDataForMonth()
    }

    private fun fetchAttendanceDataForMonth() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = appwriteAccount.get()
                val employeeId = currentUser.id
                val databaseId = "684d43c6001eb3b4547b"
                val collectionId = "684d43e5000fb8fc2d56"

                Log.d("CalendarActivity", "Fetching attendance for employeeId: $employeeId")

                val response = databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = listOf(
                        Query.equal("employeeId", employeeId)
                    )
                )

                withContext(Dispatchers.Main) {
                    allAttendanceDates.clear()
                    if (response.documents.isEmpty()) {
                        Log.d("CalendarActivity", "No attendance documents found.")
                    } else {
                        Log.d("CalendarActivity", "Fetched ${response.documents.size} attendance documents.")
                    }

                    for (document in response.documents) {
                        val timestampString = document.data["timestamp"] as? String
                        val status = document.data["status"] as? String
                        Log.d("CalendarActivity", "Processing document - Timestamp: $timestampString, Status: $status")

                        if (timestampString != null && status != null) {
                            try {
                                val date = dateFormat.parse(timestampString)
                                if (date != null) {
                                    val formattedDate = displayDateFormat.format(date)
                                    allAttendanceDates[formattedDate] = status
                                    Log.d("CalendarActivity", "Parsed and stored: $formattedDate -> $status")
                                } else {
                                    Log.w("CalendarActivity", "Date parsing returned null for timestamp: $timestampString")
                                }
                            } catch (e: Exception) {
                                Log.e("CalendarActivity", "Error parsing date '$timestampString': ${e.message}", e)
                            }
                        }
                    }
                    Log.d("CalendarActivity", "All fetched attendance data: $allAttendanceDates")

                    val currentCalendar = calendarView.currentPageDate
                    updateCalendarEvents(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH))
                    Toast.makeText(this@CalendarActivity, "Attendance data fetched.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarActivity", "Failed to fetch attendance: ${e.message}", e)
                    Toast.makeText(this@CalendarActivity, "Failed to fetch attendance: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCalendarEvents(year: Int, month: Int) {
        val events = ArrayList<CalendarDay>()
        val presentDaysInMonthList = mutableListOf<String>()

        for ((dateString, status) in allAttendanceDates) {
            val date = displayDateFormat.parse(dateString)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                    val eventCalendar = Calendar.getInstance().apply {
                        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    }
                    val calendarDay = CalendarDay(eventCalendar)

                    if (status == "P") {
                        calendarDay.backgroundDrawable = ColorDrawable(Color.GREEN)
                        events.add(calendarDay)
                        presentDaysInMonthList.add(displayDateFormat.format(eventCalendar.time))
                    } else if (status == "A") {
                        calendarDay.backgroundDrawable = ColorDrawable(Color.RED)
                        events.add(calendarDay)
                    }
                }
            }
        }

        calendarView.setCalendarDays(events)

        presentDatesTextView.text = if (presentDaysInMonthList.isNotEmpty()) {
            "Present Days: ${presentDaysInMonthList.sorted().joinToString(", ")}"
        } else {
            "Present Days: None for this month."
        }
        Log.d("CalendarActivity", "Updated presentDatesTextView with: ${presentDatesTextView.text}")
        Log.d("CalendarActivity", "Applied ${events.size} events to calendar.")
    }
} 