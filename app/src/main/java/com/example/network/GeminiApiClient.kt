package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val isQuotaOrAuth: Boolean = false) : ApiResult<Nothing>()
}

data class GeneratedImageResult(
    val bitmap: Bitmap,
    val description: String?,
    val mimeType: String
)

data class GeneratedVideoResult(
    val videoUri: String?,
    val videoBase64: String?,
    val prompt: String,
    val resolution: String
)

class GeminiApiClient(private val prefsRepo: UserPreferencesRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getEffectiveApiKey(): String {
        val customKey = prefsRepo.state.value.customApiKey.trim()
        if (customKey.isNotEmpty() && customKey != "MY_GEMINI_API_KEY") {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun isKeyConfigured(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Checks if the prompt is an owner/creator query and returns the required answer
     */
    fun checkOwnerQuestion(prompt: String): String? {
        val normalized = prompt.trim().lowercase()
        val ownerKeywords = listOf(
            "who created you",
            "who is your owner",
            "tumhara owner kaun hai",
            "tumhe kisne banaya",
            "who made you",
            "who is your creator",
            "who owns you",
            "tumhara malik kaun hai",
            "apka owner kaun hai"
        )
        for (kw in ownerKeywords) {
            if (normalized.contains(kw)) {
                return "I am MOTO AI, configured for Saif Ali Khan."
            }
        }
        return null
    }

    /**
     * Chat completion with Gemini 3.5 Flash
     */
    suspend fun chat(
        messages: List<Pair<String, String>>, // role ("user" / "model"), content
        systemInstruction: String? = null
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val lastUserMessage = messages.lastOrNull { it.first == "user" }?.second ?: ""
        val ownerAnswer = checkOwnerQuestion(lastUserMessage)
        if (ownerAnswer != null) {
            return@withContext ApiResult.Success(ownerAnswer)
        }

        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ApiResult.Error(
                "Gemini API Key is not configured. Please add your key in the AI Studio Secrets panel or Settings.",
                isQuotaOrAuth = true
            )
        }

        try {
            val root = JSONObject()

            // System instruction
            val defaultSystemPrompt = "You are MOTO AI, a futuristic and highly capable Android AI assistant configured for Saif Ali Khan. " +
                    "If asked 'Who created you?', 'Who is your owner?', or 'Tumhara owner kaun hai?', you must reply: 'I am MOTO AI, configured for Saif Ali Khan.' " +
                    "You support English, Hindi (हिन्दी), Urdu (اردو), and all major languages. Answer concisely, helpfully, and with a confident, futuristic tone."
            val activeSystem = systemInstruction ?: defaultSystemPrompt

            val systemObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", activeSystem))
            systemObj.put("parts", sysParts)
            root.put("systemInstruction", systemObj)

            // Contents array
            val contentsArray = JSONArray()
            // Send only recent turns for responsiveness
            val recentMessages = if (messages.size > 12) messages.takeLast(12) else messages
            for ((role, text) in recentMessages) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }
            root.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            root.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(root.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext ApiResult.Error(errorMsg, isQuotaOrAuth = response.code in listOf(401, 403, 429))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("text")) {
                            textBuilder.append(part.getString("text"))
                        }
                    }
                    val finalReply = textBuilder.toString().trim()
                    if (finalReply.isNotEmpty()) {
                        return@withContext ApiResult.Success(finalReply)
                    }
                }
            }
            ApiResult.Error("No response content generated by model.")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Chat call error", e)
            ApiResult.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    /**
     * Generate Image using gemini-2.5-flash-image
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        style: String? = null
    ): ApiResult<GeneratedImageResult> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ApiResult.Error("Gemini API Key missing. Please configure your key in Settings or AI Studio Secrets.", isQuotaOrAuth = true)
        }

        try {
            val enhancedPrompt = if (!style.isNullOrBlank()) {
                "$prompt, styled as $style, futuristic high quality, detailed masterpiece"
            } else {
                "$prompt, high quality, visually stunning, sharp focus"
            }

            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            parts.put(JSONObject().put("text", enhancedPrompt))
            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("TEXT")
            modalities.put("IMAGE")
            genConfig.put("responseModalities", modalities)

            val imageConfig = JSONObject()
            imageConfig.put("aspectRatio", aspectRatio)
            imageConfig.put("imageSize", "1K")
            genConfig.put("imageConfig", imageConfig)

            root.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(root.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext ApiResult.Error(errorMsg, isQuotaOrAuth = response.code in listOf(401, 403, 429))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val partsArr = content?.optJSONArray("parts")
                if (partsArr != null) {
                    var desc: String? = null
                    var foundBitmap: Bitmap? = null
                    var mimeType = "image/jpeg"

                    for (i in 0 until partsArr.length()) {
                        val part = partsArr.getJSONObject(i)
                        if (part.has("text")) {
                            desc = part.getString("text")
                        }
                        if (part.has("inlineData")) {
                            val inline = part.getJSONObject("inlineData")
                            mimeType = inline.optString("mimeType", "image/jpeg")
                            val base64Data = inline.getString("data")
                            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            foundBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        }
                    }

                    if (foundBitmap != null) {
                        return@withContext ApiResult.Success(GeneratedImageResult(foundBitmap, desc, mimeType))
                    }
                }
            }
            ApiResult.Error("Image model completed without returning image data. Try tweaking your prompt.")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Image generation error", e)
            ApiResult.Error("Error generating image: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Edit photo with multimodal instructions using gemini-2.5-flash-image
     */
    suspend fun editImage(
        originalBitmap: Bitmap,
        editInstructions: String
    ): ApiResult<GeneratedImageResult> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ApiResult.Error("Gemini API Key missing. Please configure your key in Settings or AI Studio Secrets.", isQuotaOrAuth = true)
        }

        try {
            // Compress original bitmap to JPEG bytes
            val stream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val originalBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            val promptText = "Edit and transform this image according to the following instructions: $editInstructions. Retain the core subject structure while seamlessly applying the edits."
            parts.put(JSONObject().put("text", promptText))

            val inlineObj = JSONObject()
            inlineObj.put("mimeType", "image/jpeg")
            inlineObj.put("data", originalBase64)
            parts.put(JSONObject().put("inlineData", inlineObj))

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("TEXT")
            modalities.put("IMAGE")
            genConfig.put("responseModalities", modalities)
            root.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(root.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext ApiResult.Error(errorMsg, isQuotaOrAuth = response.code in listOf(401, 403, 429))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val partsArr = content?.optJSONArray("parts")
                if (partsArr != null) {
                    var desc: String? = null
                    var editedBitmap: Bitmap? = null
                    var mimeType = "image/jpeg"

                    for (i in 0 until partsArr.length()) {
                        val part = partsArr.getJSONObject(i)
                        if (part.has("text")) {
                            desc = part.getString("text")
                        }
                        if (part.has("inlineData")) {
                            val inline = part.getJSONObject("inlineData")
                            mimeType = inline.optString("mimeType", "image/jpeg")
                            val base64Data = inline.getString("data")
                            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            editedBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        }
                    }

                    if (editedBitmap != null) {
                        return@withContext ApiResult.Success(GeneratedImageResult(editedBitmap, desc, mimeType))
                    }
                }
            }
            ApiResult.Error("Model did not return the edited image. Please refine your instructions.")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Image edit error", e)
            ApiResult.Error("Error editing image: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Video Generation using Veo (veo-3.1-fast-generate-preview)
     * Default resolution is 720p as required for mobile compatibility
     */
    suspend fun generateVideo(
        prompt: String,
        resolution: String = "720p",
        aspectRatio: String = "16:9",
        onProgress: (String) -> Unit
    ): ApiResult<GeneratedVideoResult> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ApiResult.Error("Gemini API Key missing. Please configure your key in Settings or AI Studio Secrets.", isQuotaOrAuth = true)
        }

        try {
            onProgress("Submitting video generation request to Veo ($resolution)...")

            val root = JSONObject()
            root.put("prompt", prompt)

            val config = JSONObject()
            config.put("numberOfVideos", 1)
            config.put("resolution", resolution)
            config.put("aspectRatio", aspectRatio)
            root.put("config", config)

            val submitUrl = "https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"
            val request = Request.Builder()
                .url(submitUrl)
                .post(root.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext ApiResult.Error("Veo video request failed: $errorMsg", isQuotaOrAuth = response.code in listOf(401, 403, 429))
            }

            val opJson = JSONObject(responseBody)
            val operationName = opJson.optString("name")
            if (operationName.isEmpty()) {
                return@withContext ApiResult.Error("Veo API did not return an operation ID.")
            }

            onProgress("Veo operation created. Polling generation status...")

            // Poll operation
            val maxAttempts = 30 // ~2.5 minutes
            for (attempt in 1..maxAttempts) {
                delay(5000)
                onProgress("Rendering video frames ($attempt / $maxAttempts)...")

                val pollUrl = "https://generativelanguage.googleapis.com/v1beta/$operationName?key=$apiKey"
                val pollReq = Request.Builder().url(pollUrl).get().build()
                val pollResp = client.newCall(pollReq).execute()
                val pollBody = pollResp.body?.string() ?: ""

                if (!pollResp.isSuccessful) {
                    continue
                }

                val statusJson = JSONObject(pollBody)
                if (statusJson.optBoolean("done", false)) {
                    if (statusJson.has("error")) {
                        val err = statusJson.getJSONObject("error").optString("message", "Generation error")
                        return@withContext ApiResult.Error("Veo rendering failed: $err")
                    }

                    val resp = statusJson.optJSONObject("response")
                    val generatedVideos = resp?.optJSONArray("generatedVideos")
                    if (generatedVideos != null && generatedVideos.length() > 0) {
                        val vidObj = generatedVideos.getJSONObject(0).optJSONObject("video")
                        val uri = vidObj?.optString("uri")
                        val bytes = vidObj?.optString("bytes")
                        return@withContext ApiResult.Success(
                            GeneratedVideoResult(
                                videoUri = uri,
                                videoBase64 = bytes,
                                prompt = prompt,
                                resolution = resolution
                            )
                        )
                    }
                }
            }

            ApiResult.Error("Video generation timed out. The provider may be experiencing high demand.")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Video generation error", e)
            ApiResult.Error("Video generation error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseErrorMessage(code: Int, responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val message = errObj?.optString("message") ?: "HTTP $code"
            when (code) {
                401, 403 -> "Authentication error ($code): Invalid API key or permission denied. Please verify your Gemini API key."
                429 -> "Quota limit reached ($code): Please wait a moment before trying again or check your API quota."
                else -> "API Error ($code): $message"
            }
        } catch (e: Exception) {
            "HTTP $code: $responseBody"
        }
    }
}
