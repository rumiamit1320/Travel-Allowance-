package com.example.travelallowance.location

import com.example.travelallowance.data.GeoPointRecord
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutesClient(private val apiKey: String) {
    private val client = OkHttpClient()
    suspend fun distanceKm(origin: GeoPointRecord, destination: GeoPointRecord): Double = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank() && !apiKey.startsWith("YOUR_")) { "Configure your Google Routes API key first." }
        val body = JSONObject().apply {
            put("origin", JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", origin.latitude).put("longitude", origin.longitude))))
            put("destination", JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", destination.latitude).put("longitude", destination.longitude))))
            put("travelMode", "DRIVE")
            put("routingPreference", "TRAFFIC_UNAWARE")
            put("computeAlternativeRoutes", false)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "routes.distanceMeters")
            .post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Google Routes API error ${response.code}: ${response.body?.string()}")
            val json = JSONObject(response.body!!.string())
            val routes = json.optJSONArray("routes") ?: throw IllegalStateException("No driving route found.")
            if (routes.length() == 0) throw IllegalStateException("No driving route found.")
            routes.getJSONObject(0).getDouble("distanceMeters") / 1000.0
        }
    }
}
