package com.example.travelallowance.data

data class EmployeeProfile(
    val name: String = "", val employeeId: String = "", val designation: String = "",
    val department: String = "", val officeName: String = "", val officeAddress: String = "",
    val mobile: String = "", val email: String = ""
)

data class GeoPointRecord(val latitude: Double = 0.0, val longitude: Double = 0.0, val label: String = "")

data class Journey(
    val id: String = "", val date: String = "", val start: GeoPointRecord = GeoPointRecord(),
    val destination: GeoPointRecord = GeoPointRecord(), val returnPoint: GeoPointRecord = GeoPointRecord(),
    val outwardKm: Double = 0.0, val returnKm: Double = 0.0, val totalKm: Double = 0.0,
    val taType: String = "HALF", val taAmount: Double = 0.0, val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TaSettings(val fullRate: Double = 500.0, val halfRate: Double = 250.0, val thresholdKm: Double = 80.0)
