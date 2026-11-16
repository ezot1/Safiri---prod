package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.models.UserRole
import com.example.viewmodel.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSecurityTest {

    @Test
    fun saltedPasswordHashing_usesUniqueSaltAndVerifiesCorrectly() {
        val hash1 = AppViewModel.hashPasswordSalted("StrongPass#2026")
        val hash2 = AppViewModel.hashPasswordSalted("StrongPass#2026")

        assertTrue(hash1.startsWith("pbkdf2:"))
        assertNotEquals(hash1, hash2)
        assertTrue(AppViewModel.verifyPassword("StrongPass#2026", hash1))
        assertTrue(AppViewModel.verifyPassword("StrongPass#2026", hash2))
        assertFalse(AppViewModel.verifyPassword("WrongPass#2026", hash1))
    }

    @Test
    fun signIn_unknownEmail_failsClosedWithoutAutoSignup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = AppViewModel(context as Application)
        ShadowLooper.idleMainLooper()

        var succeeded = true
        var message = ""

        viewModel.signIn("attacker@evil.example", "anything123", UserRole.ADMIN) { ok, msg ->
            succeeded = ok
            message = msg
        }

        repeat(10) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(30)
        }

        assertFalse("Unknown user should never auto-register", succeeded)
        assertNull("User session must remain null", viewModel.currentUser.value)
        assertNull("Role must remain null", viewModel.currentRole.value)
        assertTrue("Unexpected message: $message", message.contains("No account found"))
    }

    @Test
    fun signUp_rejectsPrivilegedDriverAndAdminSelfRegistration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = AppViewModel(context as Application)
        ShadowLooper.idleMainLooper()

        var adminOk = true
        var adminMsg = ""
        viewModel.signUp("Fake Admin", "fakeadmin@safiri.co.ke", UserRole.ADMIN, "email", "StrongPass#2026") { ok, msg ->
            adminOk = ok
            adminMsg = msg
        }

        repeat(5) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertFalse(adminOk)
        assertTrue("Unexpected message: $adminMsg", adminMsg.contains("provisioned by your school administrator"))

        var driverOk = true
        var driverMsg = ""
        viewModel.signUp("Fake Driver", "fakedriver@safiri.co.ke", UserRole.DRIVER, "email", "StrongPass#2026") { ok, msg ->
            driverOk = ok
            driverMsg = msg
        }

        repeat(5) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertFalse(driverOk)
        assertTrue("Unexpected message: $driverMsg", driverMsg.contains("provisioned by your school administrator"))
    }
}
