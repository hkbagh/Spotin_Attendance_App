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

class EmployeeLoginActivity : AppCompatActivity() {

    private val TAG = "EmployeeLoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_login)

        // Check if a session is already active
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = Appwrite.account.get()
                // If get() is successful, a session is active
                Appwrite.currentUser = user
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Active session found for: ${user.email}. Redirecting to dashboard.")
                    val intent = Intent(this@EmployeeLoginActivity, EmployeeDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                // No active session or error, proceed to login UI
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "No active session found or failed to retrieve session: ${e.message}")
                }
            }
        }

        val emailEditText: EditText = findViewById(R.id.employeeEmailEditText)
        val passwordEditText: EditText = findViewById(R.id.employeePasswordEditText)
        val loginButton: Button = findViewById(R.id.employeeLoginButton)
        val signupTextView: TextView = findViewById(R.id.employeeSignupTextView)

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
                        Log.d(TAG, "Login Successful for: $email")
                        Toast.makeText(this@EmployeeLoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@EmployeeLoginActivity, EmployeeDashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Close login activity
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Login Failed for: $email", e)
                        Toast.makeText(this@EmployeeLoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        signupTextView.setOnClickListener {
            val intent = Intent(this, EmployeeSignupActivity::class.java)
            startActivity(intent)
        }
    }
} 