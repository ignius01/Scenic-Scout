package com.example.ui

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getScenicAdvice(
        latitude: Double,
        longitude: Double,
        landscapeType: String,
        timeOfDay: String,
        filmStock: String,
        iso: Int,
        aperture: String,
        notes: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key not configured. Please add your GEMINI_API_KEY to your Secrets panel or .env file."
        }

        val prompt = """
            You are an expert landscape photography and analog film scouting assistant.
            Provide expert advice and scenic insights for a photographer scouting the following location:
            - Coordinates: $latitude, $longitude
            - Landscape Type: $landscapeType
            - Preferred Time of Day: $timeOfDay
            - Film Stock: ${if (filmStock.isEmpty()) "Digital Camera" else filmStock}
            - ISO: $iso
            - Aperture: $aperture
            - Additional Notes: $notes
            
            Use your maps grounding tool to get accurate geographic context, sunrise/sunset times, or interesting nearby compositions/features for these coordinates if possible.
            Provide:
            1. **Optimal Lighting Window**: golden/blue hour advice based on coordinates and preferred time.
            2. **Exposure & Camera Settings**: advice on aperture, filters (like ND/polarizers), shutter speed, and film exposure tips (e.g. reciprocity failure, exposure compensation) tailored for ${if (filmStock.isEmpty()) "digital" else filmStock}.
            3. **Compositional Inspiration**: 3 clear, highly professional bullet points of composition ideas tailored specifically for a $landscapeType landscape.
            Keep your response incredibly professional, artistic, and concise. Avoid introductory conversational bloat.
        """.trimIndent()

        // Construct request JSON using standard org.json
        try {
            val root = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // Tools - googleMaps tool as requested
            val toolsArray = JSONArray()
            val toolObj = JSONObject()
            toolObj.put("googleMaps", JSONObject())
            toolsArray.put(toolObj)
            root.put("tools", toolsArray)

            // System Instruction
            val systemInstructionObj = JSONObject()
            val systemPartsArray = JSONArray()
            val systemPartObj = JSONObject()
            systemPartObj.put("text", "You are Scenic AI Scout, a premium AI assistant for fine-art landscape photographers.")
            systemPartsArray.put(systemPartObj)
            systemInstructionObj.put("parts", systemPartsArray)
            root.put("systemInstruction", systemInstructionObj)

            val requestBodyJson = root.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiClient", "API error: $errBody")
                    return@withContext "Error: Failed to fetch advice from Gemini AI. Response code: ${response.code}"
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "No response from Gemini AI."
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text content found.")
                        }
                    }
                }
                return@withContext "No advice generated. Please try again."
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Execution error", e)
            return@withContext "Failed to connect to Gemini AI: ${e.localizedMessage}"
        }
    }
}
