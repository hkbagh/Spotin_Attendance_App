package com.hkbagh.spotin

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import android.util.TypedValue

class EmployeeAttendanceSummaryAdapter(
    private val context: Context,
    private var attendanceSummaryList: MutableList<EmployeeAttendanceSummary>,
    private var distinctAttendanceDays: List<Int> = listOf()
) : RecyclerView.Adapter<EmployeeAttendanceSummaryAdapter.EmployeeSummaryViewHolder>() {

    private val dayColumnWidth: Int = context.resources.getDimensionPixelSize(R.dimen.day_column_width)
    private val nameColumnWidth: Int = context.resources.getDimensionPixelSize(R.dimen.name_column_width)

    class EmployeeSummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val employeeNameTextView: TextView = itemView.findViewById(R.id.employeeNameTextView)
        val dailyStatusLayout: LinearLayout = itemView.findViewById(R.id.dailyStatusLayout)
        val totalPresentTextView: TextView = itemView.findViewById(R.id.totalPresentTextView)
        val totalAbsentTextView: TextView = itemView.findViewById(R.id.totalAbsentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeSummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.employee_attendance_summary_item, parent, false)
        return EmployeeSummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeSummaryViewHolder, position: Int) {
        val summary = attendanceSummaryList[position]

        // Set employee name and total counts
        holder.employeeNameTextView.text = summary.employeeName
        holder.employeeNameTextView.layoutParams = LinearLayout.LayoutParams(nameColumnWidth, LinearLayout.LayoutParams.WRAP_CONTENT)

        holder.totalPresentTextView.text = summary.totalPresent.toString()
        holder.totalAbsentTextView.text = summary.totalAbsent.toString()

        holder.dailyStatusLayout.removeAllViews() // Clear previous day views

        // Populate daily status for distinct attendance days
        for (day in distinctAttendanceDays) {
            val status = summary.dailyStatus[day] ?: "A" // Default to Absent if no data for the day
            val dayStatusTextView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dayColumnWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(if (status == "P") Color.GREEN else Color.RED)
                text = if (status == "P") "✓" else "✕"
            }
            holder.dailyStatusLayout.addView(dayStatusTextView)
        }
    }

    override fun getItemCount(): Int = attendanceSummaryList.size

    fun updateData(newAttendanceList: List<EmployeeAttendanceSummary>, newDistinctDays: List<Int>) {
        attendanceSummaryList.clear()
        attendanceSummaryList.addAll(newAttendanceList)
        distinctAttendanceDays = newDistinctDays
        notifyDataSetChanged()
    }
}
