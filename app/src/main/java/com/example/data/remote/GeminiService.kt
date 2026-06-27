package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.WeatherData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getAiInsights(weather: WeatherData): AiInsights = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or placeholder. Falling back to local rule-engine.")
            return@withContext getFallbackInsights(weather)
        }

        val prompt = """
            You are an elite, friendly AI Weather Analyst. Provide personalized health suggestions and travel advice based on the following weather data:
            City: ${weather.cityName}, ${weather.stateName}, ${weather.countryName}
            Current Temperature: ${weather.temperature}°C (Feels like: ${weather.feelsLike}°C)
            Condition: ${weather.condition} (${weather.conditionDescription})
            Humidity: ${weather.humidity}%, Pressure: ${weather.pressure} hPa, UV Index: ${weather.uvIndex}, AQI: ${weather.aqi} (${weather.aqiLabel})
            Chance of Rain: ${weather.chanceOfRain}%, Pollen: ${weather.pollenIndex} (${weather.pollenLabel})
            Wind: ${weather.windSpeed} km/h from ${weather.windDirection}

            Please output exactly a JSON object matching this structure (do not include any markdown block formatting other than the JSON itself, do not wrap in ```json):
            {
              "healthSuggestions": [
                "Detailed recommendation about pollen, exercise, hydration, or UV protection."
              ],
              "travelTips": [
                "Travel advice regarding transport delay risks, packing lists, clothing ideas, or scenic outdoor alternatives."
              ],
              "aiSummary": "A friendly 2-sentence conversational greeting and weather summary."
            }
        """.trimIndent()

        // Build Gemini API Request Body
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonString(prompt)}
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val mediaType = "application/json".toMediaType()
        val requestBody = requestJson.toRequestBody(mediaType)
        val requestUrl = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API Request failed with code: ${response.code}")
                    return@withContext getFallbackInsights(weather)
                }

                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Gemini Raw Response: $responseBodyStr")

                // Extract response text using simple JSON parsing or regex
                val textContent = extractTextFromGeminiResponse(responseBodyStr)
                if (textContent != null) {
                    try {
                        val adapter = moshi.adapter(AiInsights::class.java)
                        val insights = adapter.fromJson(textContent)
                        if (insights != null) {
                            return@withContext insights
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse insights JSON: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
        }

        return@withContext getFallbackInsights(weather)
    }

    private fun extractTextFromGeminiResponse(json: String): String? {
        // Regex to extract text inside "candidates"[0]."content"."parts"[0]."text"
        // Since Gemini returns standard structured json, we can search for the "text" field content
        val regex = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val matchResult = regex.find(json)
        val rawText = matchResult?.groupValues?.get(1) ?: return null
        
        // Unescape common characters like \n, \", etc.
        return rawText
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun escapeJsonString(input: String): String {
        return "\"" + input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }

    private fun getFallbackInsights(weather: WeatherData): AiInsights {
        val health = mutableListOf<String>()
        val travel = mutableListOf<String>()
        val summary: String

        // Temp based
        if (weather.temperature > 30f) {
            health.add("Extreme Heat Alert: Stay hydrated, drink at least 3L of water, and wear breathable light clothing.")
            health.add("UV Defense: High solar radiation. Apply SPF 50+ sunscreen and wear UV-blocking sunglasses.")
            travel.add("Transit Alert: Air conditioning is critical. Carry a thermal water bottle and avoid walking in open sun.")
            summary = "A hot and sunny day in ${weather.cityName}! Ensure you seek shade and stay extremely hydrated."
        } else if (weather.temperature < 10f) {
            health.add("Cold Weather: Layer up in wool or thermal fleece. Keep your extremities (hands, feet, ears) covered.")
            health.add("Skin Hydration: Cold air depletes humidity. Apply moisturizing cream to prevent windburn.")
            travel.add("Transit Advisory: Expect icy/wet road conditions. Drive cautiously and allow extra time for commuting.")
            summary = "A crisp, cool day in ${weather.cityName}. Bundle up warmly before heading outdoors!"
        } else {
            health.add("Ideal Outdoor Conditions: Perfect weather for outdoor jogging, breathing exercises, and natural vitamin D.")
            health.add("Active Lifestyle: Pollen levels are moderate. Take normal allergy precautions if sensitive.")
            travel.add("Travel Suggestion: Ideal day for sightseeing, walking tours, or renting a bike to explore the city.")
            summary = "Pleasant and balanced weather in ${weather.cityName}. A wonderful day to enjoy outdoor activities!"
        }

        // Condition based
        when {
            weather.condition.contains("Rain", ignoreCase = true) || weather.condition.contains("Drizzle", ignoreCase = true) -> {
                health.add("Indoor Exercises Recommended: High humidity and damp air can aggravate respiratory sensitivity.")
                travel.add("Slick Roads: Road surfaces will be highly slippery. Carry an automatic umbrella and wear water-resistant shoes.")
                travel.add("Flight/Transit Delays: Heavy rain may cause local traffic snarls and brief flight scheduling delays.")
            }
            weather.condition.contains("Snow", ignoreCase = true) -> {
                health.add("Warmth Focus: Frostbite risks on exposed skin after 30 minutes. Wear insulated waterproof gloves.")
                travel.add("Hazardous Roadways: Slippage risk is severe. Double your safety distance and carry emergency snow chains.")
            }
            weather.condition.contains("Thunder", ignoreCase = true) -> {
                health.add("Lightning Safety: Seek solid shelter immediately. Do not stand under trees or hold metal poles.")
                travel.add("Transit Stop: Severe storm cells may cause power grid flickers or local street flooding.")
            }
        }

        // AQI based
        if (weather.aqi > 100) {
            health.add("Air Quality Advisory: Elevated AQI levels (${weather.aqi}). Sensitive groups should wear N95 masks.")
            health.add("Avoid Heavy Cardio: Limit outdoor strenuous exercise to protect lung and cardiovascular health.")
            travel.add("Closed Windows: Keep vehicle windows rolled up and turn on internal air circulation/recirc mode.")
        }

        return AiInsights(
            healthSuggestions = health,
            travelTips = travel,
            aiSummary = summary
        )
    }
}

data class AiInsights(
    val healthSuggestions: List<String>,
    val travelTips: List<String>,
    val aiSummary: String
)
