package com.hkbagh.spotin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hkbagh.spotin.Appwrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import io.appwrite.Query

class EmployerLoginActivity : AppCompatActivity() {

    private val TAG = "EmployerLoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_login)

        // Check if a session is already active
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = Appwrite.account.get()
                // If get() is successful, a session is active
                Appwrite.currentUser = user
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Active session found for: ${user.email}. Checking profile...")
                    checkUserProfileAndNavigate(user.id, UserType.EMPLOYER)
                }
            } catch (e: Exception) {
                // No active session or error, proceed to login UI
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "No active session found or failed to retrieve session: ${e.message}")
                }
            }
        }

        val emailEditText: EditText = findViewById(R.id.employerEmailEditText)
        val passwordEditText: EditText = findViewById(R.id.employerPasswordEditText)
        val loginButton: Button = findViewById(R.id.employerLoginButton)
        val signupTextView: TextView = findViewById(R.id.employerSignupTextView)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d(TAG, "Login button clicked. Email: $email")

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Email or password is empty.")
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Attempting to create email/password session for: $email")
                    Appwrite.account.createEmailPasswordSession(email = email, password = password)
                    val user = Appwrite.account.get() // Fetch the user instance after successful session creation
                    Appwrite.currentUser = user // Save the user instance globally
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Login Successful for: $email. Checking profile...")
                        checkUserProfileAndNavigate(user.id, UserType.EMPLOYER)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Login Failed for: $email", e)
                        Toast.makeText(this@EmployerLoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        signupTextView.setOnClickListener {
            val intent = Intent(this, EmployerSignupActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun checkUserProfileAndNavigate(userId: String, userType: UserType) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "checkUserProfileAndNavigate called for userId: $userId, userType: $userType")
            Log.d(TAG, "Using DATABASE_ID: ${Appwrite.APPWRITE_DATABASE_ID}, COLLECTION_ID: ${Appwrite.APPWRITE_COLLECTION_ID}")
            try {
                val response = Appwrite.databases.listDocuments(
                    databaseId = Appwrite.APPWRITE_DATABASE_ID,
                    collectionId = Appwrite.APPWRITE_COLLECTION_ID,
                    queries = listOf(Query.equal("user_id", userId))
                )

                Log.d(TAG, "listDocuments response: ${response.documents}")

                if (response.documents.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "User profile not found for userId: $userId. Redirecting to UserProfileActivity.")
                        Toast.makeText(this@EmployerLoginActivity, "Please complete your profile", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@EmployerLoginActivity, UserProfileActivity::class.java)
                        intent.putExtra("USER_TYPE", userType.name)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                } else {
                    val userProfile = response.documents[0].data
                    val employeeId = userProfile["employee_id"] as? String
                    val name = userProfile["name"] as? String
                    val phoneNo = userProfile["phone_no"] as? String

                    Log.d(TAG, "Existing profile data: employee_id=$employeeId, name=$name, phone_no=$phoneNo")

                    if (employeeId.isNullOrEmpty() || name.isNullOrEmpty() || phoneNo.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "User profile incomplete for userId: $userId. Redirecting to UserProfileActivity.")
                            Toast.makeText(this@EmployerLoginActivity, "Please complete your profile", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@EmployerLoginActivity, UserProfileActivity::class.java)
                            intent.putExtra("USER_TYPE", userType.name)
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "User profile complete for userId: $userId. Redirecting to Employer Dashboard.")
                            Toast.makeText(this@EmployerLoginActivity, "Profile loaded, redirecting to dashboard", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@EmployerLoginActivity, EmployerDashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error checking user profile for userId: $userId", e)
                    Toast.makeText(this@EmployerLoginActivity, "Error checking profile: ${e.message}", Toast.LENGTH_LONG).show()
                    // Optionally, redirect to a generic error screen or allow user to retry
                }
            }
        }
    }
}

enum class UserType {
    EMPLOYER, EMPLOYEE
} 