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
    fun `demo fixtures are only loaded in debug builds`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val viewModel = AppViewModel(app)

        if (BuildConfig.DEBUG) {
            assertTrue(viewModel.students.value.isNotEmpty())
            assertTrue(viewModel.activeAlerts.value.isNotEmpty())
            assertTrue(viewModel.adminFleetBuses.value.isNotEmpty())
        } else {
            assertTrue(viewModel.students.value.isEmpty())
            assertTrue(viewModel.activeAlerts.value.isEmpty())
            assertTrue(viewModel.adminFleetBuses.value.isEmpty())
            assertTrue(viewModel.transitLines.value.isEmpty())
            assertTrue(viewModel.invoiceHistory.value.isEmpty())
            assertEquals(SubscriptionStatus.INACTIVE, viewModel.subscriptionStatus.value)

            viewModel.updateLiveDriverLocation(lat = -1.2905, lng = 36.77875, speedMetersPerSec = 10f)
            assertEquals(0f, viewModel.progress.value, 0f)
            assertEquals(0, viewModel.etaMinutes.value)

            viewModel.triggerSOS()
            assertFalse(viewModel.sosActive.value)
            assertTrue(viewModel.rateLimitNotice.value?.contains("bus assignment") == true)
        }
    }

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
    fun `paySubscriptionInvoice validates phone and does not record local invoices`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val viewModel = AppViewModel(app)
        val initialCount = viewModel.invoiceHistory.value.size

        viewModel.paySubscriptionInvoice("12345")
        assertTrue(viewModel.subscriptionToast.value?.contains("valid Kenyan M-Pesa number") == true)
        assertEquals(initialCount, viewModel.invoiceHistory.value.size)

        viewModel.paySubscriptionInvoice("0712345678")
        assertTrue(viewModel.subscriptionToast.value?.contains("authorized school administrator") == true)
        assertEquals(initialCount, viewModel.invoiceHistory.value.size)
    }

    @Test
    fun `notification channels initialize cleanly and respect permission check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SafiriNotificationManager.ensureChannels(context)
        // Robolectric defaults POST_NOTIFICATIONS to denied on API 33+ unless granted
        assertFalse(SafiriNotificationManager.hasNotificationPermission(context))
    }
}
