package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object SafiriNotificationManager {

    const val CHANNEL_BOARDING = "safiri_boarding_alerts"
    const val CHANNEL_EMERGENCY = "safiri_emergency_sos"
    const val CHANNEL_LOCATION_TRACKING = "safiri_location_tracking"
    private const val TAG = "SafiriNotifications"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                val boardingChannel = NotificationChannel(
                    CHANNEL_BOARDING,
                    "Shuttle Boarding & ETA Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time notifications when your child boards, arrives, or shuttle delays occur."
                }

                val emergencyChannel = NotificationChannel(
                    CHANNEL_EMERGENCY,
                    "Emergency SOS & Fleet Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent driver SOS broadcasts and critical route advisories."
                    enableVibration(true)
                }

                val locationChannel = NotificationChannel(
                    CHANNEL_LOCATION_TRACKING,
                    "Live Driver Location",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shown while a driver shares live bus location."
                    setShowBadge(false)
                }

                manager.createNotificationChannel(boardingChannel)
                manager.createNotificationChannel(emergencyChannel)
                manager.createNotificationChannel(locationChannel)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to initialize notification channels: ${e.message}")
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val appContext = context.applicationContext
        ensureChannels(appContext)
        if (!hasNotificationPermission(appContext)) return

        try {
            val priority = if (channelId == CHANNEL_EMERGENCY) {
                NotificationCompat.PRIORITY_MAX
            } else {
                NotificationCompat.PRIORITY_HIGH
            }

            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification skipped due to missing runtime permission: ${se.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post notification: ${e.message}")
        }
    }
}
