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
                    Log.d(TAG, "Active session found for: ${user.email}. Redirecting.")
                    Toast.makeText(this@EmployerLoginActivity, "Already logged in as Employer", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to Employer Dashboard or appropriate activity
                    // For now, just finish this activity if a session is active
                    // finish()
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
                        Log.d(TAG, "Login Successful for: $email")
                        Toast.makeText(this@EmployerLoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        // Navigate to Employer Dashboard or next activity
                        // For now, no dashboard for employer, so just a toast.
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
} 