package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRepository {

    private const val TAG = "GeminiRepository"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var customApiKey: String? = null

    fun setCustomApiKey(key: String) {
        customApiKey = key.trim()
    }

    private fun getApiKey(): String {
        if (!customApiKey.isNull_or_blank()) {
            return customApiKey!!
        }
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isNull_or_blank() || key == "MY_GEMINI_API_KEY") {
            ""
        } else {
            key
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    /**
     * Low-Latency Fast Assistant using gemini-3.1-flash-lite-preview
     */
    suspend fun generateFastResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-flash-lite-preview"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
            })
        }
        return@withContext executeRequest(model, jsonPayload)
    }

    /**
     * Maps Grounded Response using gemini-3.5-flash with Google Maps Tool
     */
    suspend fun generateMapsGroundedResponse(prompt: String, userLocation: String = "Nairobi, Kenya"): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.5-flash"
        val fullPrompt = "Context location: $userLocation. $prompt"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", fullPrompt))
                    })
                })
            })
            // Add Google Maps Tool Grounding
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            })
        }
        return@withContext executeRequest(model, jsonPayload)
    }

    /**
     * High Thinking Mode using gemini-3.1-pro-preview with thinkingLevel set to HIGH
     * Note: Do NOT set maxOutputTokens
     */
    suspend fun generateHighThinkingResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-pro-preview"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                })
                // Explicitly DO NOT set maxOutputTokens as instructed
            })
        }
        return@withContext executeRequest(model, jsonPayload)
    }

    /**
     * General tasks using gemini-3.5-flash
     */
    suspend fun generateGeneralResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.5-flash"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }
        return@withContext executeRequest(model, jsonPayload)
    }

    /**
     * Complex Reasoning & Analysis using gemini-3.1-pro-preview
     */
    suspend fun generateComplexAnalysis(prompt: String): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-pro-preview"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }
        return@withContext executeRequest(model, jsonPayload)
    }

    private fun executeRequest(
        model: String,
        payload: JSONObject,
        retryCount: Int = 0
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return "Note: GEMINI_API_KEY is not set or configured. Please set GEMINI_API_KEY in the Secrets panel."
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = payload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error ($model): ${response.code} - $responseString")

                    // Handle 429 Rate Limit / Quota Exhaustion specifically
                    if (response.code == 429) {
                        if (retryCount < 2) {
                            // Retry after short backoff delay
                            Thread.sleep(1500L * (retryCount + 1))
                            return executeRequest(model, payload, retryCount + 1)
                        }

                        // Try fallback to standard robust models if rate limited on preview models
                        val fallbackModel = when {
                            model != "gemini-2.5-flash" -> "gemini-2.5-flash"
                            else -> "gemini-1.5-flash"
                        }
                        if (model != fallbackModel && retryCount <= 2) {
                            Log.w(TAG, "Rate limited on $model. Retrying with fallback model $fallbackModel...")
                            // Simplify payload for fallback model by removing model-specific thinking/tools if present
                            val simplePayload = JSONObject().apply {
                                put("contents", payload.optJSONArray("contents"))
                            }
                            return executeRequest(fallbackModel, simplePayload, retryCount + 1)
                        }

                        return "Quota Exceeded / Rate Limit (429): The Gemini API rate limit for this key or model ($model) has been reached. Please wait a few seconds before trying again, or check your API key quota in AI Studio."
                    }

                    // Handle 404 or 400 (e.g. model name or specific tool/thinking config not supported on standard key)
                    if (response.code == 404 || response.code == 400) {
                        val fallbackModel = "gemini-2.5-flash"
                        if (model != fallbackModel && retryCount == 0) {
                            Log.w(TAG, "Model $model returned ${response.code}. Retrying with $fallbackModel...")
                            val simplePayload = JSONObject().apply {
                                put("contents", payload.optJSONArray("contents"))
                            }
                            return executeRequest(fallbackModel, simplePayload, retryCount = 1)
                        }
                    }

                    return "Gemini API Error (${response.code}): ${response.message}\n${responseString.take(200)}"
                }

                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCand = candidates.getJSONObject(0)
                    val content = firstCand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val textBuilder = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val partObj = parts.getJSONObject(i)
                            if (partObj.has("text")) {
                                textBuilder.append(partObj.getString("text"))
                            }
                        }
                        if (textBuilder.isNotEmpty()) {
                            return textBuilder.toString()
                        }
                    }
                }
                "No output content generated by $model."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed Gemini API call for $model", e)
            "Unable to reach Gemini AI service: ${e.localizedMessage ?: "Network request failed"}"
        }
    }
}
