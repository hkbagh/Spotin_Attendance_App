package com.hkbagh.spotin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.appwrite.models.Document
import java.text.SimpleDateFormat
import java.util.*

class EmployeeAttendanceAdapter(private var attendanceList: MutableList<Document<Map<String, Any?>>>) :
    RecyclerView.Adapter<EmployeeAttendanceAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val employeeIdTextView: TextView = itemView.findViewById(R.id.employeeIdTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val locationCoordTextView: TextView = itemView.findViewById(R.id.locationCoordTextView)
        val ipAddressTextView: TextView = itemView.findViewById(R.id.ipAddressTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.employee_attendance_item, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val attendance = attendanceList[position]

        val employeeId = attendance.data["employeeId"] as? String ?: "N/A"
        val timestamp = attendance.data["timestamp"] as? String
        val latitude = attendance.data["latitude"] as? Double
        val longitude = attendance.data["longitude"] as? Double
        val ipAddress = attendance.data["ip"] as? String ?: "N/A"

        holder.employeeIdTextView.text = "Employee ID: $employeeId"
        holder.ipAddressTextView.text = "IP Address: $ipAddress"

        if (timestamp != null) {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = parser.parse(timestamp)
                if (date != null) {
                    holder.timestampTextView.text = "Timestamp: ${formatter.format(date)}"
                } else {
                    holder.timestampTextView.text = "Timestamp: Invalid"
                }
            } catch (e: Exception) {
                holder.timestampTextView.text = "Timestamp: Error parsing"
                e.printStackTrace()
            }
        } else {
            holder.timestampTextView.text = "Timestamp: N/A"
        }

        if (latitude != null && longitude != null) {
            holder.locationCoordTextView.text = String.format(Locale.getDefault(), "Location: %.4f, %.4f", latitude, longitude)
        } else {
            holder.locationCoordTextView.text = "Location: N/A"
        }
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateData(newAttendanceList: List<Document<Map<String, Any?>>>) {
        attendanceList.clear()
        attendanceList.addAll(newAttendanceList)
        notifyDataSetChanged()
    }
}
