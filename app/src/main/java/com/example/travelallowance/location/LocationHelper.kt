package com.example.travelallowance.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import com.example.travelallowance.data.GeoPointRecord

class LocationHelper(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission")
    suspend fun current(label: String): GeoPointRecord {
        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: throw IllegalStateException("Unable to obtain GPS location. Check GPS signal and try again.")
        return GeoPointRecord(location.latitude, location.longitude, label)
    }
}
