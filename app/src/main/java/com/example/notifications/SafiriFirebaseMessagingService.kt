package com.example.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.database.SafiriFirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SafiriFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            SafiriFirestoreRepository(this@SafiriFirebaseMessagingService)
                .saveDeviceMessagingToken(uid, token)
                .onFailure { error ->
                    Log.e(TAG, "Could not register refreshed FCM device token", error)
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data["type"] != ALERT_MESSAGE_TYPE) return

        val title = message.notification?.title ?: "Safiri bus alert"
        val body = message.notification?.body ?: DEFAULT_ALERT_BODY
        val severity = message.data["severity"]
        val channelId = if (severity == "DANGER") {
            SafiriNotificationManager.CHANNEL_EMERGENCY
        } else {
            SafiriNotificationManager.CHANNEL_BOARDING
        }
        val notificationId = message.data["alertId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        SafiriNotificationManager.sendNotification(
            context = this,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            body = body
        )
    }

    companion object {
        private const val TAG = "SafiriMessaging"
        private const val ALERT_MESSAGE_TYPE = "bus_alert"
        private const val DEFAULT_ALERT_BODY = "An active bus alert is available. Open Safiri for details."
    }
}
