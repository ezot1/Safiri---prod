package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.location.DriverLocationTracker
import com.example.models.SubscriptionStatus
import com.example.notifications.SafiriNotificationManager
import com.example.viewmodel.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelemetryAndPaymentTest {

    @Test
    fun `route progress computes bounds along Nairobi shuttle corridor`() {
        val startProgress = DriverLocationTracker.computeRouteProgress(-1.2825, 36.7450)
        val endProgress = DriverLocationTracker.computeRouteProgress(-1.2985, 36.8125)
        val midProgress = DriverLocationTracker.computeRouteProgress(-1.2905, 36.77875)

        assertEquals(0f, startProgress, 0.02f)
        assertEquals(1f, endProgress, 0.02f)
        assertTrue("Expected midpoint near 0.5, got $midProgress", midProgress in 0.45f..0.55f)
        assertEquals(0f, DriverLocationTracker.estimateRemainingDistanceKm(1f), 0.01f)
        assertTrue(DriverLocationTracker.estimateRemainingDistanceKm(0f) > 7.0f)
    }

    @Test
    fun `updateLiveDriverLocation updates GPS state and metrics`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val viewModel = AppViewModel(app)
        ShadowLooper.idleMainLooper()

        viewModel.updateLiveDriverLocation(lat = -1.2905, lng = 36.77875, speedMetersPerSec = 10f)

        assertEquals(-1.2905, viewModel.driverGpsLat.value, 0.0001)
        assertEquals(36.77875, viewModel.driverGpsLng.value, 0.0001)
        assertEquals(36, viewModel.driverSpeedKmh.value)
        assertTrue(viewModel.progress.value in 0.45f..0.55f)
    }

    @Test
    fun `paySubscriptionInvoice rejects invalid phone or amount`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val viewModel = AppViewModel(app)
        val initialCount = viewModel.invoiceHistory.value.size

        viewModel.paySubscriptionInvoice("12345", "15000")
        assertTrue(viewModel.subscriptionToast.value?.contains("valid Kenyan M-Pesa number") == true)
        assertEquals(initialCount, viewModel.invoiceHistory.value.size)

        viewModel.paySubscriptionInvoice("0712345678", "0")
        assertTrue(viewModel.subscriptionToast.value?.contains("greater than 0 KES") == true)
        assertEquals(initialCount, viewModel.invoiceHistory.value.size)
    }

    @Test
    fun `paySubscriptionInvoice records sandbox preview instead of fake paid invoice`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val viewModel = AppViewModel(app)
        viewModel.setSubscriptionStatus(SubscriptionStatus.INACTIVE)
        val initialCount = viewModel.invoiceHistory.value.size

        viewModel.paySubscriptionInvoice("+254712345678", "15000")
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)

        assertEquals(initialCount + 1, viewModel.invoiceHistory.value.size)
        val newest = viewModel.invoiceHistory.value.first()
        assertEquals("SANDBOX_PREVIEW", newest.status)
        assertEquals(SubscriptionStatus.INACTIVE, viewModel.subscriptionStatus.value)
        assertTrue(viewModel.subscriptionToast.value?.contains("Sandbox preview") == true)
    }

    @Test
    fun `notification channels initialize cleanly and respect permission check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SafiriNotificationManager.ensureChannels(context)
        // Robolectric defaults POST_NOTIFICATIONS to denied on API 33+ unless granted
        assertFalse(SafiriNotificationManager.hasNotificationPermission(context))
    }
}
