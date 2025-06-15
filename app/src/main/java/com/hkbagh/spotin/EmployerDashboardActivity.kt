package com.hkbagh.spotin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Databases
import io.appwrite.models.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EmployerDashboardActivity : AppCompatActivity() {

    private lateinit var employerWelcomeTextView: TextView
    private lateinit var employeeSearchView: SearchView
    private lateinit var presentEmployeesRecyclerView: RecyclerView
    private lateinit var employeeAdapter: EmployeeAttendanceAdapter
    private lateinit var employerLogoutButton: Button
    private lateinit var viewAllAttendanceButton: Button

    private lateinit var appwriteClient: Client
    private lateinit var databases: Databases

    private var allPresentEmployees = mutableListOf<io.appwrite.models.Document<Map<String, Any?>>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_dashboard)

        employerWelcomeTextView = findViewById(R.id.employerWelcomeTextView)
        employeeSearchView = findViewById(R.id.employeeSearchView)
        presentEmployeesRecyclerView = findViewById(R.id.presentEmployeesRecyclerView)
        employerLogoutButton = findViewById(R.id.employerLogoutButton)
        viewAllAttendanceButton = findViewById(R.id.viewAllAttendanceButton)

        appwriteClient = Client(applicationContext)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("684c2359000d2bf66544") // Use your actual Appwrite project ID
            .setSelfSigned(true)

        databases = Databases(appwriteClient)

        // Setup RecyclerView
        employeeAdapter = EmployeeAttendanceAdapter(mutableListOf())
        presentEmployeesRecyclerView.layoutManager = LinearLayoutManager(this)
        presentEmployeesRecyclerView.adapter = employeeAdapter

        // Fetch and set welcome message using Appwrite.currentUser and UserProfile
        fetchEmployerProfileAndDisplayWelcome()

        // Setup SearchView listener
        employeeSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterEmployees(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterEmployees(newText)
                return true
            }
        })

        employerLogoutButton.setOnClickListener {
            logoutEmployer()
        }

        viewAllAttendanceButton.setOnClickListener {
            val intent = Intent(this@EmployerDashboardActivity, AllEmployeesAttendanceActivity::class.java)
            startActivity(intent)
        }

        fetchPresentEmployeesToday()
    }

    private fun fetchPresentEmployeesToday() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val databaseId = Appwrite.APPWRITE_DATABASE_ID // Your attendance database ID
                val collectionId = Appwrite.APPWRITE_COLLECTION_ID // Your attendance collection ID

                // Get today's date in UTC for filtering
                val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val startOfDay = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000XXX", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(today.time)
                val endOfDay = SimpleDateFormat("yyyy-MM-dd'T'23:59:59.999XXX", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(today.time)

                val response = databases.listDocuments(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    queries = listOf(
                        Query.equal("status", "P"),
                        Query.greaterThanEqual("timestamp", startOfDay),
                        Query.lessThanEqual("timestamp", endOfDay),
                        Query.orderDesc("\$createdAt")
                    )
                )

                withContext(Dispatchers.Main) {
                    allPresentEmployees.clear()
                    if (response.documents.isEmpty()) {
                        Log.d("EmployerDashboard", "No present employees found for today.")
                        Toast.makeText(this@EmployerDashboardActivity, "No employees present today.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("EmployerDashboard", "Fetched ${response.documents.size} present employees.")
                        allPresentEmployees.addAll(response.documents as List<io.appwrite.models.Document<Map<String, Any?>>>)
                    }
                    employeeAdapter.updateData(allPresentEmployees)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EmployerDashboard", "Failed to fetch present employees: ${e.message}", e)
                    Toast.makeText(this@EmployerDashboardActivity, "Failed to fetch data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filterEmployees(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allPresentEmployees
        } else {
            allPresentEmployees.filter { document ->
                (document.data["employee_id"] as? String)?.contains(query, ignoreCase = true) == true // Corrected attribute name
            }.toMutableList()
        }
        employeeAdapter.updateData(filteredList)
    }

    private fun logoutEmployer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Appwrite.account.deleteSession(sessionId = "current")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployerDashboardActivity, "Logged out as Employer", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@EmployerDashboardActivity, EmployerLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EmployerDashboard", "Logout failed: ${e.message}", e)
                    Toast.makeText(this@EmployerDashboardActivity, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchEmployerProfileAndDisplayWelcome() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = Appwrite.currentUser.id
                val response = databases.listDocuments(
                    databaseId = Appwrite.APPWRITE_DATABASE_ID,
                    collectionId = Appwrite.APPWRITE_COLLECTION_ID,
                    queries = listOf(Query.equal("user_id", userId))
                )

                withContext(Dispatchers.Main) {
                    if (response.documents.isNotEmpty()) {
                        val userProfile = response.documents[0].data
                        val name = userProfile["name"] as? String
                        val employeeId = userProfile["employee_id"] as? String
                        val welcomeMessage = if (!name.isNullOrEmpty() && !employeeId.isNullOrEmpty()) {
                            "Welcome, $name! (ID: $employeeId)"
                        } else if (!name.isNullOrEmpty()) {
                            "Welcome, $name!"
                        } else if (!employeeId.isNullOrEmpty()) {
                            "Welcome, ID: $employeeId!"
                        } else {
                            "Welcome, Employer!"
                        }
                        employerWelcomeTextView.text = welcomeMessage
                    } else {
                        employerWelcomeTextView.text = "Welcome, Employer! (Profile not found)"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EmployerDashboard", "Failed to fetch employer profile: ${e.message}", e)
                    employerWelcomeTextView.text = "Welcome, Employer! (Error loading profile)"
                }
            }
        }
    }
}
