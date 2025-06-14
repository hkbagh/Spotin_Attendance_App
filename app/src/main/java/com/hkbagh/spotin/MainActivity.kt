package com.hkbagh.spotin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.content.Intent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Appwrite.init(applicationContext)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) {
                v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val employerLoginButton: Button = findViewById(R.id.employerLoginButton)
        val employeeLoginButton: Button = findViewById(R.id.employeeLoginButton)

        employerLoginButton.setOnClickListener {
            // Handle Employer Login click
            // For now, let's just show a toast or navigate to a new activity
            // Toast.makeText(this, "Employer Login Clicked", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, EmployerLoginActivity::class.java))
        }

        employeeLoginButton.setOnClickListener {
            // Handle Employee Login click
            // For now, let's just show a toast or navigate to a new activity
            // Toast.makeText(this, "Employee Login Clicked", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, EmployeeLoginActivity::class.java))
        }
    }
}