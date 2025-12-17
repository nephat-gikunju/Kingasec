package com.example.kingasec

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Data classes for OpenAI API request and response
@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double
)

@Serializable
data class Message(
    val role: String, // e.g., "system" or "user"
    val content: String
)

@Serializable
data class OpenAiResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

// Retrofit interface defining the OpenAI API endpoint
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

/**
 * A singleton object to handle communication with the OpenAI API.
 * It provides a simple function to get a risk analysis for a given app.
 */
object ContextChecker {
    private const val BASE_URL = "https://api.openai.com/"

    // Lazy initialization of the Retrofit service
    private val retrofitService: OpenAiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true } // Ignore fields not defined in our data classes

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenAiApi::class.java)
    }

    /**
     * Analyzes an app's permissions using the OpenAI API and returns a user-friendly explanation.
     *
     * @param appName The name of the application.
     * @param appCategory The category of the application (e.g., "Finance", "Game").
     * @param permissions A list of permissions requested by the app.
     * @return A string containing the AI-generated analysis, or null on failure.
     */
    suspend fun getRiskAnalysis(appName: String, appCategory: String, permissions: List<String>): String? {
        // Use the API key from BuildConfig
        val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"

        // Construct the prompt for the AI model
        val prompt = createPrompt(appName, appCategory, permissions)

        val request = OpenAiRequest(
            model = "gpt-3.5-turbo", // Or a newer model like "gpt-4"
            messages = listOf(
                Message("system", "You are a mobile security analyst. Your task is to explain the potential risks of app permissions to a non-technical user. Be concise and clear."),
                Message("user", prompt)
            ),
            maxTokens = 150, // Limit the length of the response
            temperature = 0.5 // Adjust for more or less creative responses
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = retrofitService.getCompletion(apiKey, request)
                response.choices.firstOrNull()?.message?.content?.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                null // Return null if the API call fails
            }
        }
    }

    private fun createPrompt(
        appName: String,
        appCategory: String,
        permissions: List<String>
    ): String {
        val permissionString = permissions.joinToString(", ")

        return """
        Analyze the permissions for the app "$appName", which is in the "$appCategory" category.

        The app requests the following potentially dangerous permissions: $permissionString.

        Explain the potential privacy or security risks. For example, why is it concerning for a $appCategory app to have access to certain permissions? Be specific and easy to understand.
        """
    }
}