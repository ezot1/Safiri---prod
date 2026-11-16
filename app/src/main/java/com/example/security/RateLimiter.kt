package com.example.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Abstraction for persisting rate-limit counters and lockouts across app restarts.
 */
interface RateLimitStore {
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesRateLimitStore(context: Context) : RateLimitStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("safiri_rate_limits", Context.MODE_PRIVATE)

    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

class InMemoryRateLimitStore : RateLimitStore {
    private val longs = mutableMapOf<String, Long>()
    private val ints = mutableMapOf<String, Int>()
    private val strings = mutableMapOf<String, String>()

    override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
    override fun putLong(key: String, value: Long) { longs[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun remove(key: String) {
        longs.remove(key)
        ints.remove(key)
        strings.remove(key)
    }
}

data class RateLimitDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long = 0L,
    val remainingQuota: Int? = null,
    val message: String = ""
)

/**
 * Thread-safe, persistent client-side rate limiter covering Auth, Gemini AI, SOS, Alerts,
 * Interportal Messaging, and Community Incident reporting.
 */
class AppRateLimiter(
    private val store: RateLimitStore,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val lock = Any()

    companion object {
        const val MAX_AI_PROMPT_CHARS = 800
        const val MAX_MESSAGE_CHARS = 500
        const val MAX_INCIDENT_TITLE_CHARS = 200

        // Auth policies
        const val SIGN_IN_COOLDOWN_MS = 2_000L
        const val SIGN_IN_MAX_FAILURES = 5
        const val SIGN_IN_FAIL_WINDOW_MS = 15 * 60_000L
        private val LOCKOUT_DURATIONS_MS = longArrayOf(60_000L, 5 * 60_000L, 15 * 60_000L)

        const val SIGN_UP_COOLDOWN_MS = 10_000L
        const val SIGN_UP_HOURLY_MAX = 5
        const val ONE_HOUR_MS = 60 * 60_000L
        const val ONE_DAY_MS = 24 * 60 * 60_000L

        // AI policies
        const val AI_STANDARD_COOLDOWN_MS = 4_000L
        const val AI_HIGH_THINKING_COOLDOWN_MS = 10_000L
        const val AI_BURST_MAX_PER_MIN = 6
        const val AI_DAILY_QUOTA = 40

        // Operational policies
        const val SOS_COOLDOWN_MS = 30_000L
        const val ALERT_COOLDOWN_MS = 5_000L
        const val ALERT_MAX_PER_10_MIN = 10
        const val MESSAGE_COOLDOWN_MS = 2_000L
        const val MESSAGE_MAX_PER_5_MIN = 20
        const val INCIDENT_COOLDOWN_MS = 15_000L
        const val INCIDENT_MAX_PER_HOUR = 5
    }

    // -------------------------------------------------------------------------
    // 1. AUTHENTICATION RATE LIMITING (Persistent brute-force protection)
    // -------------------------------------------------------------------------

    fun checkSignIn(email: String): RateLimitDecision = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(email)

        val lockUntil = store.getLong("auth_lock_until_$keyId", 0L)
        if (now < lockUntil) {
            val remainingSec = ceilSeconds(lockUntil - now)
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Too many failed attempts. Account locked for $remainingSec seconds."
            )
        }

        val lastAttempt = store.getLong("auth_last_attempt_$keyId", 0L)
        if (lastAttempt > 0L && now - lastAttempt < SIGN_IN_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(SIGN_IN_COOLDOWN_MS - (now - lastAttempt))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Please wait a moment before trying again."
            )
        }

        store.putLong("auth_last_attempt_$keyId", now)
        return RateLimitDecision(allowed = true)
    }

    fun recordSignInOutcome(email: String, success: Boolean) = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(email)
        store.putLong("auth_last_attempt_$keyId", now)

        if (success) {
            store.remove("auth_fail_count_$keyId")
            store.remove("auth_fail_window_start_$keyId")
            store.remove("auth_lock_until_$keyId")
            store.remove("auth_lock_tier_$keyId")
        } else {
            val windowStart = store.getLong("auth_fail_window_start_$keyId", 0L)
            val prevFails = if (windowStart == 0L || now - windowStart > SIGN_IN_FAIL_WINDOW_MS) {
                store.putLong("auth_fail_window_start_$keyId", now)
                0
            } else {
                store.getInt("auth_fail_count_$keyId", 0)
            }

            val nextFails = prevFails + 1
            if (nextFails >= SIGN_IN_MAX_FAILURES) {
                val tier = store.getInt("auth_lock_tier_$keyId", 0)
                val durationMs = LOCKOUT_DURATIONS_MS[tier.coerceAtMost(LOCKOUT_DURATIONS_MS.lastIndex)]
                store.putLong("auth_lock_until_$keyId", now + durationMs)
                store.putInt("auth_lock_tier_$keyId", (tier + 1).coerceAtMost(LOCKOUT_DURATIONS_MS.lastIndex))
                store.putInt("auth_fail_count_$keyId", 0)
                store.putLong("auth_fail_window_start_$keyId", now)
            } else {
                store.putInt("auth_fail_count_$keyId", nextFails)
            }
        }
    }

    fun checkAndConsumeSignUp(email: String): RateLimitDecision = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(email)

        val lastAttempt = store.getLong("signup_last_$keyId", 0L)
        if (lastAttempt > 0L && now - lastAttempt < SIGN_UP_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(SIGN_UP_COOLDOWN_MS - (now - lastAttempt))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Please wait $remainingSec seconds between registration attempts."
            )
        }

        val burst = checkAndUpdateSlidingWindow(
            bucketKey = "signup_device_window",
            windowMs = ONE_HOUR_MS,
            maxCount = SIGN_UP_HOURLY_MAX,
            now = now
        )
        if (!burst.allowed) {
            return burst.copy(
                message = "Hourly registration limit reached. Please try again in ${burst.retryAfterSeconds}s."
            )
        }

        store.putLong("signup_last_$keyId", now)
        return RateLimitDecision(allowed = true, remainingQuota = burst.remainingQuota)
    }

    // -------------------------------------------------------------------------
    // 2. GEMINI AI RATE LIMITING & QUOTA
    // -------------------------------------------------------------------------

    fun getRemainingDailyAiQuota(userKey: String): Int = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(userKey.ifBlank { "guest" })
        val dailyTimestamps = pruneTimestamps(
            readTimestamps("ai_daily_$keyId"),
            now - ONE_DAY_MS
        )
        (AI_DAILY_QUOTA - dailyTimestamps.size).coerceAtLeast(0)
    }

    fun tryConsumeAiQuery(
        userKey: String,
        prompt: String,
        isHighThinking: Boolean = false
    ): RateLimitDecision = synchronized(lock) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return RateLimitDecision(allowed = false, message = "Please enter a prompt first.")
        }
        if (trimmed.length > MAX_AI_PROMPT_CHARS) {
            return RateLimitDecision(
                allowed = false,
                message = "Prompt exceeds the $MAX_AI_PROMPT_CHARS-character limit (${trimmed.length}/$MAX_AI_PROMPT_CHARS)."
            )
        }

        val now = clockMs()
        val keyId = normalizeKey(userKey.ifBlank { "guest" })
        val cooldownMs = if (isHighThinking) AI_HIGH_THINKING_COOLDOWN_MS else AI_STANDARD_COOLDOWN_MS

        val lastQuery = store.getLong("ai_last_$keyId", 0L)
        if (lastQuery > 0L && now - lastQuery < cooldownMs) {
            val remainingSec = ceilSeconds(cooldownMs - (now - lastQuery))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                remainingQuota = getRemainingDailyAiQuota(userKey),
                message = "AI cooldown active. Please wait ${remainingSec}s before sending another query."
            )
        }

        // Check per-minute burst without consuming daily quota yet
        val burstTimestamps = pruneTimestamps(readTimestamps("ai_burst_$keyId"), now - 60_000L)
        if (burstTimestamps.size >= AI_BURST_MAX_PER_MIN) {
            val retryMs = (burstTimestamps.first() + 60_000L) - now
            val retrySec = ceilSeconds(retryMs)
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = retrySec,
                remainingQuota = getRemainingDailyAiQuota(userKey),
                message = "AI burst limit reached (max $AI_BURST_MAX_PER_MIN/min). Try again in ${retrySec}s."
            )
        }

        // Check daily quota
        val dailyTimestamps = pruneTimestamps(readTimestamps("ai_daily_$keyId"), now - ONE_DAY_MS)
        if (dailyTimestamps.size >= AI_DAILY_QUOTA) {
            val retryMs = (dailyTimestamps.first() + ONE_DAY_MS) - now
            val retrySec = ceilSeconds(retryMs)
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = retrySec,
                remainingQuota = 0,
                message = "Daily AI quota reached ($AI_DAILY_QUOTA queries/24h). Resets in ${retrySec / 60}m."
            )
        }

        // Commit consumption
        store.putLong("ai_last_$keyId", now)
        writeTimestamps("ai_burst_$keyId", burstTimestamps + now)
        val updatedDaily = dailyTimestamps + now
        writeTimestamps("ai_daily_$keyId", updatedDaily)

        return RateLimitDecision(
            allowed = true,
            remainingQuota = (AI_DAILY_QUOTA - updatedDaily.size).coerceAtLeast(0)
        )
    }

    // -------------------------------------------------------------------------
    // 3. OPERATIONAL RATE LIMITING (SOS, ALERTS, MESSAGES, INCIDENTS)
    // -------------------------------------------------------------------------

    fun tryConsumeSos(busPlate: String): RateLimitDecision = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(busPlate)
        val lastSos = store.getLong("sos_last_$keyId", 0L)
        if (lastSos > 0L && now - lastSos < SOS_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(SOS_COOLDOWN_MS - (now - lastSos))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "SOS already broadcast. Please wait ${remainingSec}s before re-triggering."
            )
        }
        store.putLong("sos_last_$keyId", now)
        return RateLimitDecision(allowed = true)
    }

    fun tryConsumeAlert(userKey: String): RateLimitDecision = synchronized(lock) {
        val now = clockMs()
        val keyId = normalizeKey(userKey.ifBlank { "admin" })
        val lastAlert = store.getLong("alert_last_$keyId", 0L)
        if (lastAlert > 0L && now - lastAlert < ALERT_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(ALERT_COOLDOWN_MS - (now - lastAlert))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Please wait ${remainingSec}s before broadcasting another alert."
            )
        }
        val window = checkAndUpdateSlidingWindow(
            bucketKey = "alert_win_$keyId",
            windowMs = 10 * 60_000L,
            maxCount = ALERT_MAX_PER_10_MIN,
            now = now
        )
        if (!window.allowed) {
            return window.copy(
                message = "Alert broadcast limit reached (max $ALERT_MAX_PER_10_MIN per 10m). Wait ${window.retryAfterSeconds}s."
            )
        }
        store.putLong("alert_last_$keyId", now)
        return RateLimitDecision(allowed = true, remainingQuota = window.remainingQuota)
    }

    fun tryConsumeMessage(userKey: String, content: String): RateLimitDecision = synchronized(lock) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return RateLimitDecision(allowed = false, message = "Message cannot be empty.")
        }
        if (trimmed.length > MAX_MESSAGE_CHARS) {
            return RateLimitDecision(
                allowed = false,
                message = "Message exceeds $MAX_MESSAGE_CHARS characters."
            )
        }
        val now = clockMs()
        val keyId = normalizeKey(userKey.ifBlank { "user" })
        val lastMsg = store.getLong("msg_last_$keyId", 0L)
        if (lastMsg > 0L && now - lastMsg < MESSAGE_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(MESSAGE_COOLDOWN_MS - (now - lastMsg))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Sending too fast. Please wait ${remainingSec}s."
            )
        }
        val window = checkAndUpdateSlidingWindow(
            bucketKey = "msg_win_$keyId",
            windowMs = 5 * 60_000L,
            maxCount = MESSAGE_MAX_PER_5_MIN,
            now = now
        )
        if (!window.allowed) {
            return window.copy(
                message = "Message rate limit reached. Please wait ${window.retryAfterSeconds}s."
            )
        }
        store.putLong("msg_last_$keyId", now)
        return RateLimitDecision(allowed = true, remainingQuota = window.remainingQuota)
    }

    fun tryConsumeIncident(userKey: String, title: String): RateLimitDecision = synchronized(lock) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return RateLimitDecision(allowed = false, message = "Incident title cannot be empty.")
        }
        if (trimmed.length > MAX_INCIDENT_TITLE_CHARS) {
            return RateLimitDecision(
                allowed = false,
                message = "Incident title exceeds $MAX_INCIDENT_TITLE_CHARS characters."
            )
        }
        val now = clockMs()
        val keyId = normalizeKey(userKey.ifBlank { "user" })
        val lastInc = store.getLong("inc_last_$keyId", 0L)
        if (lastInc > 0L && now - lastInc < INCIDENT_COOLDOWN_MS) {
            val remainingSec = ceilSeconds(INCIDENT_COOLDOWN_MS - (now - lastInc))
            return RateLimitDecision(
                allowed = false,
                retryAfterSeconds = remainingSec,
                message = "Please wait ${remainingSec}s before reporting another incident."
            )
        }
        val window = checkAndUpdateSlidingWindow(
            bucketKey = "inc_win_$keyId",
            windowMs = ONE_HOUR_MS,
            maxCount = INCIDENT_MAX_PER_HOUR,
            now = now
        )
        if (!window.allowed) {
            return window.copy(
                message = "Hourly incident report limit reached (max $INCIDENT_MAX_PER_HOUR/hr). Wait ${window.retryAfterSeconds}s."
            )
        }
        store.putLong("inc_last_$keyId", now)
        return RateLimitDecision(allowed = true, remainingQuota = window.remainingQuota)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun checkAndUpdateSlidingWindow(
        bucketKey: String,
        windowMs: Long,
        maxCount: Int,
        now: Long
    ): RateLimitDecision {
        val active = pruneTimestamps(readTimestamps(bucketKey), now - windowMs)
        if (active.size >= maxCount) {
            val oldest = active.first()
            val retrySec = ceilSeconds((oldest + windowMs) - now)
            return RateLimitDecision(allowed = false, retryAfterSeconds = retrySec, remainingQuota = 0)
        }
        val updated = active + now
        writeTimestamps(bucketKey, updated)
        return RateLimitDecision(allowed = true, remainingQuota = (maxCount - updated.size).coerceAtLeast(0))
    }

    private fun readTimestamps(key: String): List<Long> {
        val raw = store.getString(key, "")
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun writeTimestamps(key: String, timestamps: List<Long>) {
        store.putString(key, timestamps.joinToString(","))
    }

    private fun pruneTimestamps(timestamps: List<Long>, cutoffMs: Long): List<Long> {
        return timestamps.filter { it >= cutoffMs }.sorted()
    }

    private fun ceilSeconds(ms: Long): Long {
        if (ms <= 0L) return 1L
        return ((ms + 999L) / 1000L).coerceAtLeast(1L)
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim().lowercase().replace(Regex("[^a-z0-9_@.-]"), "_")
    }
}
