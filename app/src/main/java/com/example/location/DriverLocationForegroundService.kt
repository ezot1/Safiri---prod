package com.example.location

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.database.SafiriFirestoreRepository
import com.example.notifications.SafiriNotificationManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class DriverLocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        SafiriNotificationManager.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        try {
            val notification = buildTrackingNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (error: Exception) {
            DriverLocationTracker.publishTrackingError(
                error.localizedMessage ?: "Could not start background location sharing."
            )
            Log.e(TAG, "Failed to start location foreground service", error)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (locationJob?.isActive != true) {
            locationJob = serviceScope.launch { collectAndSyncLocation() }
        }
        return START_STICKY
    }

    private suspend fun collectAndSyncLocation() {
        if (!DriverLocationTracker.hasLocationPermission(this)) {
            failTracking("Location permission is required to share the bus location.")
            return
        }

        val driverUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (error: Exception) {
            Log.e(TAG, "Firebase Authentication is unavailable to the location service", error)
            null
        }
        if (driverUid.isNullOrBlank()) {
            failTracking("Sign in again before starting live location sharing.")
            return
        }

        val repository = try {
            SafiriFirestoreRepository(this)
        } catch (error: Exception) {
            Log.e(TAG, "Firestore is unavailable to the location service", error)
            failTracking("The live location service is unavailable. Try again later.")
            return
        }

        val busResult = repository.getFleetBusForDriver(driverUid)
        val bus = busResult.getOrElse { error ->
            Log.e(TAG, "Could not load the driver's assigned bus", error)
            failTracking("Could not verify your bus assignment. Try again later.")
            return
        }
        if (bus == null) {
            failTracking("Live sharing requires a bus assignment from your school.")
            return
        }

        DriverLocationTracker.clearTrackingError()
        var lastTelemetrySyncMs = 0L
        try {
            DriverLocationTracker.locationUpdates(this).collect { location ->
                DriverLocationTracker.publishLocation(location)
                val now = SystemClock.elapsedRealtime()
                if (now - lastTelemetrySyncMs >= TELEMETRY_INTERVAL_MS) {
                    lastTelemetrySyncMs = now
                    repository.updateBusLocationByDriver(
                        busId = bus.id,
                        lat = location.latitude,
                        lng = location.longitude,
                        speed = (location.speed * METERS_PER_SECOND_TO_KMH)
                            .toLong()
                            .coerceAtLeast(0L)
                    ).onFailure { error ->
                        Log.e(TAG, "Failed to sync driver location to Firestore", error)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Driver location updates stopped unexpectedly", error)
            failTracking(error.localizedMessage ?: "Unable to read or sync the device location.")
        }
    }

    private fun failTracking(message: String) {
        DriverLocationTracker.publishTrackingError(message)
        stopSelf()
    }

    private fun buildTrackingNotification() =
        NotificationCompat.Builder(this, SafiriNotificationManager.CHANNEL_LOCATION_TRACKING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Safiri live location is active")
            .setContentText("Sharing your bus location while you drive.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationJob?.cancel()
        serviceScope.cancel()
        DriverLocationTracker.clearPublishedLocation()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.location.action.START"
        const val ACTION_STOP = "com.example.location.action.STOP"
        private const val TAG = "DriverLocationService"
        private const val NOTIFICATION_ID = 4210
        private const val TELEMETRY_INTERVAL_MS = 10_000L
        private const val METERS_PER_SECOND_TO_KMH = 3.6f
    }
}
