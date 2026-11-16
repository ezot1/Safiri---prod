package com.example.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object GeminiRepository {

    private const val TAG = "GeminiRepository"
    private const val MAX_PROMPT_CHARS = 800
    private const val AI_TIMEOUT_MS = 60_000L

    private val inFlightMutex = Mutex()

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.3f
            }
        )
    }

    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private fun validatePrompt(prompt: String): String? {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return "Please enter a prompt first."
        if (trimmed.length > MAX_PROMPT_CHARS) {
            return "Prompt is too long (${trimmed.length}/$MAX_PROMPT_CHARS characters)."
        }
        return null
    }

    /**
     * Low-Latency Fast Assistant using gemini-3.5-flash-lite via Firebase AI Logic.
     */
    suspend fun generateFastResponse(prompt: String): String = withContext(Dispatchers.IO) {
        validatePrompt(prompt)?.let { return@withContext it }
        executeGuarded {
            val response = withTimeout(AI_TIMEOUT_MS) {
                fastModel.generateContent(prompt.trim())
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Model returned an empty response."
        }
    }

    /**
     * Location-aware Transit Assistant using gemini-3.8-flash via Firebase AI Logic.
     */
    suspend fun generateMapsGroundedResponse(
        prompt: String,
        userLocation: String = "Nairobi, Kenya"
    ): String = withContext(Dispatchers.IO) {
        validatePrompt(prompt)?.let { return@withContext it }
        val fullPrompt = "Transit location context: $userLocation. Provide practical local route and traffic guidance for this query: ${prompt.trim()}"
        executeGuarded {
            val response = withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContent(fullPrompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Model returned an empty response."
        }
    }

    /**
     * High Thinking / Structured Safety Analysis using gemini-3.8-flash via Firebase AI Logic.
     */
    suspend fun generateHighThinkingResponse(prompt: String): String = withContext(Dispatchers.IO) {
        validatePrompt(prompt)?.let { return@withContext it }
        val analysisPrompt = "Think step by step and provide a structured safety, risk, and contingency analysis for: ${prompt.trim()}"
        executeGuarded {
            val response = withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContent(analysisPrompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Model returned an empty response."
        }
    }

    /**
     * General tasks using gemini-3.8-flash via Firebase AI Logic.
     */
    suspend fun generateGeneralResponse(prompt: String): String = withContext(Dispatchers.IO) {
        validatePrompt(prompt)?.let { return@withContext it }
        executeGuarded {
            val response = withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContent(prompt.trim())
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Model returned an empty response."
        }
    }

    /**
     * Complex Reasoning & Analysis using gemini-3.8-flash via Firebase AI Logic.
     */
    suspend fun generateComplexAnalysis(prompt: String): String = withContext(Dispatchers.IO) {
        validatePrompt(prompt)?.let { return@withContext it }
        val analysisPrompt = "Provide a detailed analytical breakdown and actionable recommendations for: ${prompt.trim()}"
        executeGuarded {
            val response = withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContent(analysisPrompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Model returned an empty response."
        }
    }

    private suspend fun executeGuarded(block: suspend () -> String): String {
        if (!inFlightMutex.tryLock()) {
            return "An AI request is already in progress. Please wait for it to finish."
        }
        return try {
            block()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            "Generation timed out after 60 seconds. Please try again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            e.localizedMessage ?: "Firebase AI generation failed."
        } finally {
            inFlightMutex.unlock()
        }
    }
}
