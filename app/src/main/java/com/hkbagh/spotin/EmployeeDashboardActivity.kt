package com.hkbagh.spotin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.appwrite.ID
import io.appwrite.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.NetworkInterface
import java.util.Collections

class EmployeeDashboardActivity : AppCompatActivity() {

    private lateinit var welcomeTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var markPresentButton: Button
    private lateinit var logoutButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_dashboard)

        welcomeTextView = findViewById(R.id.welcomeTextView)
        locationTextView = findViewById(R.id.locationTextView)
        markPresentButton = findViewById(R.id.markPresentButton)
        logoutButton = findViewById(R.id.logoutButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get current logged-in user
        // The user instance is now saved in Appwrite.currentUser after login
        welcomeTextView.text = "Welcome, ${Appwrite.currentUser.name.ifEmpty { Appwrite.currentUser.email }}!"

        markPresentButton.setOnClickListener { recordAttendance("P") }
        logoutButton.setOnClickListener { logoutUser() }

        checkLocationPermissionsAndGetLocation()
    }

    private fun getLocalIpAddress(): String {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces()).filterNotNull()
            for (intf in networkInterfaces) {
                val addresses = Collections.list(intf.getInetAddresses()).filterNotNull()
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr.isSiteLocalAddress && !addr.isLinkLocalAddress && !addr.isMulticastAddress) {
                        // Check for IPv4 address
                        if (addr.hostAddress.indexOf(':') < 0) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("IPAddress", "Error getting IP: ${ex.message}")
        }
        return "Unknown"
    }

    private fun checkLocationPermissionsAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLatestLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLatestLocation()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
                locationTextView.text = "Location: Permission Denied"
            }
        }
    }

    private fun getLatestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    lastKnownLocation = location
                    locationTextView.text = "Location: ${location.latitude}, ${location.longitude}"
                } else {
                    locationTextView.text = "Location: Not available"
                }
            }
            .addOnFailureListener { e ->
                locationTextView.text = "Location: Error (${e.message})"
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun recordAttendance(status: String) {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Please wait, getting location...", Toast.LENGTH_SHORT).show()
            checkLocationPermissionsAndGetLocation() // Try to get location again
            return
        }

        val latitude = lastKnownLocation?.latitude ?: 0.0
        val longitude = lastKnownLocation?.longitude ?: 0.0
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        val ipAddress = getLocalIpAddress()

        // Get current user ID
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use Appwrite.currentUser which is set after login
                val employeeId = Appwrite.currentUser.id
                val data = mapOf(
                    "employeeId" to employeeId,
                    "status" to status,
                    "timestamp" to timestamp,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "ip" to ipAddress
                )

                val databaseId = "684d43c6001eb3b4547b"
                val collectionId = "684d43e5000fb8fc2d56"

                Appwrite.databases.createDocument(
                    databaseId = databaseId,
                    collectionId = collectionId,
                    documentId = ID.unique(),
                    data = data,
                    permissions = listOf(
                        "read(\"user:$employeeId\")",
                        "update(\"user:$employeeId\")"
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployeeDashboardActivity, "Attendance recorded as $status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Attendance", "Failed to record attendance: ${e.message}", e)
                    Toast.makeText(this@EmployeeDashboardActivity, "Failed to record attendance: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun logoutUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Appwrite.account.deleteSession(sessionId = "current")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployeeDashboardActivity, "Logged out", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@EmployeeDashboardActivity, EmployeeLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployeeDashboardActivity, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
} 