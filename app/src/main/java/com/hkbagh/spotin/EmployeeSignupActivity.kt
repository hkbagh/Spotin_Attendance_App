package com.hkbagh.spotin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.appwrite.ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hkbagh.spotin.Appwrite
import io.appwrite.models.User

class EmployeeSignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_signup)

        val emailEditText: EditText = findViewById(R.id.employeeSignupEmailEditText)
        val passwordEditText: EditText = findViewById(R.id.employeeSignupPasswordEditText)
        val signupButton: Button = findViewById(R.id.employeeSignupButton)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Appwrite.account.create(
                        userId = ID.unique(),
                        email = email,
                        password = password
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmployeeSignupActivity, "Signup Successful", Toast.LENGTH_SHORT).show()
                        finish() // Go back to login screen
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmployeeSignupActivity, "Signup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
} 