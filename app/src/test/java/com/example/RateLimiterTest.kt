package com.example

import com.example.security.AppRateLimiter
import com.example.security.InMemoryRateLimitStore
import org.junit.Assert.*
import org.junit.Test

class RateLimiterTest {

    @Test
    fun authSignIn_locksOutAfterFiveFailures_andPersistsAcrossRestarts() {
        var nowMs = 1_000_000L
        val persistentStore = InMemoryRateLimitStore()
        var limiter = AppRateLimiter(persistentStore) { nowMs }

        repeat(5) {
            assertTrue(limiter.checkSignIn("parent@safiri.co.ke").allowed)
            limiter.recordSignInOutcome("parent@safiri.co.ke", success = false)
            nowMs += 3_000L // beyond 2s attempt cooldown
        }

        // Simulate app restart with a fresh AppRateLimiter reading from the same persistent store
        limiter = AppRateLimiter(persistentStore) { nowMs }
        val lockedDecision = limiter.checkSignIn("parent@safiri.co.ke")
        assertFalse(lockedDecision.allowed)
        assertTrue(lockedDecision.retryAfterSeconds > 0L)

        // Advance past 60s lockout and verify successful sign-in clears failure counters
        nowMs += 61_000L
        assertTrue(limiter.checkSignIn("parent@safiri.co.ke").allowed)
        limiter.recordSignInOutcome("parent@safiri.co.ke", success = true)

        nowMs += 3_000L
        assertTrue(limiter.checkSignIn("parent@safiri.co.ke").allowed)
    }

    @Test
    fun aiQuery_enforcesPromptLimit_cooldown_burst_andDailyQuota() {
        var nowMs = 10_000_000L
        val store = InMemoryRateLimitStore()
        val limiter = AppRateLimiter(store) { nowMs }

        // Oversized prompt blocked
        val longPrompt = "a".repeat(AppRateLimiter.MAX_AI_PROMPT_CHARS + 1)
        assertFalse(limiter.tryConsumeAiQuery("u1", longPrompt).allowed)

        // Standard query succeeds
        val first = limiter.tryConsumeAiQuery("u1", "Where is bus KDE 732X?")
        assertTrue(first.allowed)
        assertEquals(AppRateLimiter.AI_DAILY_QUOTA - 1, first.remainingQuota)

        // Immediate repeat blocked by 4s cooldown
        nowMs += 1_000L
        assertFalse(limiter.tryConsumeAiQuery("u1", "Second prompt").allowed)

        // Burst limit: 6 per minute
        for (i in 2..AppRateLimiter.AI_BURST_MAX_PER_MIN) {
            nowMs += 5_000L
            assertTrue("Query $i should succeed", limiter.tryConsumeAiQuery("u1", "Prompt $i").allowed)
        }
        nowMs += 5_000L // still within 60s of first query (5*5s + 1s = 26s)
        val burstBlocked = limiter.tryConsumeAiQuery("u1", "Burst overflow")
        assertFalse(burstBlocked.allowed)

        // Advance 61s and consume remaining daily quota
        nowMs += 61_000L
        var used = AppRateLimiter.AI_BURST_MAX_PER_MIN
        while (used < AppRateLimiter.AI_DAILY_QUOTA) {
            assertTrue(limiter.tryConsumeAiQuery("u1", "Prompt $used").allowed)
            used++
            nowMs += 12_000L // avoid both cooldown and 6/min burst
        }

        val dailyBlocked = limiter.tryConsumeAiQuery("u1", "Over daily cap")
        assertFalse(dailyBlocked.allowed)
        assertEquals(0, limiter.getRemainingDailyAiQuota("u1"))

        // Advance 24 hours -> daily quota resets
        nowMs += AppRateLimiter.ONE_DAY_MS + 1_000L
        assertEquals(AppRateLimiter.AI_DAILY_QUOTA, limiter.getRemainingDailyAiQuota("u1"))
        assertTrue(limiter.tryConsumeAiQuery("u1", "Fresh day").allowed)
    }

    @Test
    fun operationalActions_throttleSosAlertsMessagesAndIncidents() {
        var nowMs = 50_000_000L
        val limiter = AppRateLimiter(InMemoryRateLimitStore()) { nowMs }

        // SOS 30s cooldown
        assertTrue(limiter.tryConsumeSos("KDE 732X").allowed)
        nowMs += 5_000L
        assertFalse(limiter.tryConsumeSos("KDE 732X").allowed)
        nowMs += 26_000L
        assertTrue(limiter.tryConsumeSos("KDE 732X").allowed)

        // Alert 5s cooldown
        assertTrue(limiter.tryConsumeAlert("admin@safiri.co.ke").allowed)
        assertFalse(limiter.tryConsumeAlert("admin@safiri.co.ke").allowed)
        nowMs += 5_000L
        assertTrue(limiter.tryConsumeAlert("admin@safiri.co.ke").allowed)

        // Message length and 2s cooldown
        assertFalse(limiter.tryConsumeMessage("p1", "x".repeat(501)).allowed)
        assertTrue(limiter.tryConsumeMessage("p1", "On our way").allowed)
        assertFalse(limiter.tryConsumeMessage("p1", "Wait").allowed)
        nowMs += 2_000L
        assertTrue(limiter.tryConsumeMessage("p1", "Wait").allowed)

        // Incident 15s cooldown
        assertTrue(limiter.tryConsumeIncident("p1", "Traffic on Ngong Rd").allowed)
        nowMs += 3_000L
        assertFalse(limiter.tryConsumeIncident("p1", "Another report").allowed)
        nowMs += 13_000L
        assertTrue(limiter.tryConsumeIncident("p1", "Another report").allowed)
    }
}
