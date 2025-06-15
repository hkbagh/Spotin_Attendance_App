package com.hkbagh.spotin

data class EmployeeAttendanceSummary(
    val employeeId: String,
    val employeeName: String,
    val dailyStatus: Map<Int, String>, // Day of month (1-31) to status ("P" or "A")
    val totalPresent: Int,
    val totalAbsent: Int
)
