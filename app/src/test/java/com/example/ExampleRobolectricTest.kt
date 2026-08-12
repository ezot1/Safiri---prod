package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Safiri", appName)
  }

  @Test
  fun `test sign-in rate limiting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = com.example.viewmodel.AppViewModel(context as android.app.Application)

    var message = ""
    
    // First sign in attempt
    viewModel.signIn("parent@safiri.co.ke", "demo1234", com.example.models.UserRole.PARENT) { success, msg ->
       if (!success) message = msg
    }
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Attempting immediately again (within 2 seconds) should trigger the rapid rate limit
    viewModel.signIn("parent@safiri.co.ke", "demo1234", com.example.models.UserRole.PARENT) { success, msg ->
       if (!success) message = msg
    }
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    
    assertEquals("Please wait a moment before trying again.", message)
  }

  @Test
  fun `test sign-in with mismatching portal role is rejected`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.database.AppDatabase.getDatabase(context)
    val userDao = db.userDao()
    
    // Explicitly insert the user to ensure deterministic setup
    val testEmail = "test_driver@safiri.co.ke"
    val hashedPassword = com.example.viewmodel.AppViewModel.hashPassword("test1234")
    kotlinx.coroutines.runBlocking {
        userDao.insertUser(
            com.example.database.UserEntity(
                email = testEmail,
                name = "Test Driver",
                role = "DRIVER",
                provider = "email",
                passwordHash = hashedPassword
            )
        )
    }

    val viewModel = com.example.viewmodel.AppViewModel(context as android.app.Application)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    var message = ""
    var signinSuccess = true

    // Attempting to log in as a Driver into the PARENT portal
    viewModel.signIn(testEmail, "test1234", com.example.models.UserRole.PARENT) { success, msg ->
        signinSuccess = success
        message = msg
    }
    
    // Pump main looper a few times to allow background IO thread query and main thread resume to finish
    repeat(10) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Thread.sleep(30)
    }

    // Assert that the sign in was rejected and correct portal instruction is returned
    assertFalse("Expected sign-in to fail, but it succeeded. message: '$message'", signinSuccess)
    assertTrue("Actual message: '$message'", message.contains("This account is registered as a Driver. Please use the correct login portal."))
  }
}
