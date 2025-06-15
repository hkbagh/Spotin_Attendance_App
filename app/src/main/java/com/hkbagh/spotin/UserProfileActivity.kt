package com.hkbagh.spotin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userType = intent.getStringExtra("USER_TYPE")

        val employeeIdEditText: EditText = findViewById(R.id.employeeIdEditText)
        val nameEditText: EditText = findViewById(R.id.nameEditText)
        val phoneNoEditText: EditText = findViewById(R.id.phoneNoEditText)
        val saveProfileButton: Button = findViewById(R.id.saveProfileButton)

        saveProfileButton.setOnClickListener {
            val employeeId = employeeIdEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val phoneNo = phoneNoEditText.text.toString().trim()

            if (employeeId.isEmpty() || name.isEmpty() || phoneNo.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = Appwrite.currentUser.id

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = mapOf(
                        "employee_id" to employeeId,
                        "name" to name,
                        "phone_no" to phoneNo,
                        "user_id" to userId // Associate the profile with the Appwrite user
                    )

                    Appwrite.databases.createDocument(
                        databaseId = Appwrite.APPWRITE_DATABASE_ID,
                        collectionId = Appwrite.APPWRITE_COLLECTION_ID,
                        documentId = ID.unique(),
                        data = data,
                        permissions = listOf(
                            Permission.read(Role.user(userId)),
                            Permission.update(Role.user(userId))
                        )
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserProfileActivity, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                        val intent: Intent = when (userType) {
                            UserType.EMPLOYER.name -> Intent(this@UserProfileActivity, EmployerDashboardActivity::class.java)
                            UserType.EMPLOYEE.name -> Intent(this@UserProfileActivity, EmployeeDashboardActivity::class.java)
                            else -> Intent(this@UserProfileActivity, MainActivity::class.java) // Fallback or error
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserProfileActivity, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}