package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DriverLocationTracker {

    private const val UPDATE_INTERVAL_MS = 5000L
    private const val MIN_UPDATE_INTERVAL_MS = 3000L

    // Nairobi Shuttle Corridor endpoints (Kawangware Stop -> Upper Hill Junction)
    private const val ROUTE_START_LAT = -1.2825
    private const val ROUTE_START_LNG = 36.7450
    private const val ROUTE_END_LAT = -1.2985
    private const val ROUTE_END_LNG = 36.8125
    private const val TOTAL_ROUTE_KM = 7.7

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(context: Context): Flow<Location> = callbackFlow {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val client = LocationServices.getFusedLocationProviderClient(appContext)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.lastLocation.addOnSuccessListener { last ->
            if (last != null) {
                trySend(last)
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { e -> close(e) }

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }

    fun computeRouteProgress(lat: Double, lng: Double): Float {
        val dx = ROUTE_END_LNG - ROUTE_START_LNG
        val dy = ROUTE_END_LAT - ROUTE_START_LAT
        val lenSq = dx * dx + dy * dy
        if (lenSq <= 0.0) return 0f
        val proj = ((lng - ROUTE_START_LNG) * dx + (lat - ROUTE_START_LAT) * dy) / lenSq
        return proj.toFloat().coerceIn(0f, 1f)
    }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun estimateRemainingDistanceKm(progress: Float): Float {
        val remaining = (1f - progress.coerceIn(0f, 1f)) * TOTAL_ROUTE_KM
        return (kotlin.math.round(remaining * 10.0) / 10.0).toFloat()
    }
}
