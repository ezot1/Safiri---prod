package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun passwordHashing_isSecureAndConsistent() {
    val plainPassword = "demo1234"
    val hash = com.example.viewmodel.AppViewModel.hashPassword(plainPassword)
    
    // Hash should be a valid SHA-256 hex string (64 characters long)
    assertEquals(64, hash.length)
    assertTrue(hash.matches(Regex("[0-9a-fA-F]{64}")))

    // Hash of the same password should always be identical
    val secondHash = com.example.viewmodel.AppViewModel.hashPassword(plainPassword)
    assertEquals(hash, secondHash)

    // Different passwords must yield different hashes
    val differentHash = com.example.viewmodel.AppViewModel.hashPassword("otherpassword")
    assertNotEquals(hash, differentHash)
  }
}
