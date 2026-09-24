package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LiveGoldPrices(
    val timestamp: Long,
    val currency: String,
    val priceOunce: Double,
    val priceGram24k: Double,
    val priceGram22k: Double,
    val priceGram21k: Double,
    val priceGram18k: Double,
    val priceGram14k: Double,
    val priceGram12k: Double,
    val change: Double = 0.0,
    val changePercentage: Double = 0.0
) {
    fun toKaratMap(): Map<Int, Double> {
        return mapOf(
            24 to priceGram24k,
            22 to priceGram22k,
            21 to priceGram21k,
            18 to priceGram18k,
            14 to priceGram14k,
            12 to priceGram12k
        )
    }
}

object GoldApiClient {
    const val DEFAULT_API_KEY = "goldapi-a5ec4bacb3a44f8746e621896ece2554-io"
    private const val BASE_URL = "https://www.goldapi.io/api"
    private const val TROY_OUNCE_TO_GRAMS = 31.1034768

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchGoldPrices(
        apiKey: String = DEFAULT_API_KEY,
        currency: String = "EGP",
        symbol: String = "XAU"
    ): Result<LiveGoldPrices> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim().ifBlank { DEFAULT_API_KEY }
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("يرجى إدخال مفتاح API الخاص بـ GoldAPI.io / Please enter your GoldAPI key")
            )
        }

        val cleanCurrency = currency.trim().uppercase()
        val url = "$BASE_URL/$symbol/$cleanCurrency"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("x-access-token", trimmedKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val statusCode = response.code

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val json = JSONObject(body)
                        json.optString("error").ifBlank { json.optString("message", "") }
                    } catch (_: Exception) {
                        ""
                    }

                    val message = when (statusCode) {
                        401, 403 -> "مفتاح API غير صالح أو منتهي الصلاحية (${if (errorMsg.isNotBlank()) errorMsg else "Unauthorized"}). يرجى التأكد من المفتاح في goldapi.io"
                        429 -> "تم تجاوز الحد المسموح من الطلبات على GoldAPI.io (Quota limit exceeded)."
                        404 -> "لم يتم العثور على بيانات العملة $cleanCurrency أو الرمز $symbol في GoldAPI."
                        else -> "فشل جلب الأسعار من GoldAPI.io (رمز الخطأ: $statusCode ${if (errorMsg.isNotBlank()) "- $errorMsg" else ""})"
                    }
                    return@withContext Result.failure(Exception(message))
                }

                val json = JSONObject(body)

                val priceOunce = json.optDouble("price", 0.0)
                var g24 = json.optDouble("price_gram_24k", 0.0)
                var g22 = json.optDouble("price_gram_22k", 0.0)
                var g21 = json.optDouble("price_gram_21k", 0.0)
                var g18 = json.optDouble("price_gram_18k", 0.0)
                var g14 = json.optDouble("price_gram_14k", 0.0)

                // If per-gram 24k wasn't returned directly, derive from ounce price
                if (g24 <= 0.0 && priceOunce > 0.0) {
                    g24 = priceOunce / TROY_OUNCE_TO_GRAMS
                }

                if (g24 > 0.0) {
                    if (g22 <= 0.0) g22 = g24 * (22.0 / 24.0)
                    if (g21 <= 0.0) g21 = g24 * (21.0 / 24.0)
                    if (g18 <= 0.0) g18 = g24 * (18.0 / 24.0)
                    if (g14 <= 0.0) g14 = g24 * (14.0 / 24.0)
                }

                val g12 = if (g24 > 0.0) g24 * (12.0 / 24.0) else 0.0
                val ch = json.optDouble("ch", 0.0)
                val chp = json.optDouble("chp", 0.0)
                val timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000)

                if (g24 <= 0.0 && priceOunce <= 0.0) {
                    return@withContext Result.failure(
                        Exception("لم يتم استلام أسعار صحيحة من GoldAPI.io للعملة $cleanCurrency")
                    )
                }

                Result.success(
                    LiveGoldPrices(
                        timestamp = timestamp,
                        currency = cleanCurrency,
                        priceOunce = priceOunce,
                        priceGram24k = g24,
                        priceGram22k = g22,
                        priceGram21k = g21,
                        priceGram18k = g18,
                        priceGram14k = g14,
                        priceGram12k = g12,
                        change = ch,
                        changePercentage = chp
                    )
                )
            }
        } catch (e: Exception) {
            val friendlyError = if (e.message?.contains("Unable to resolve host", ignoreCase = true) == true) {
                "تعذر الاتصال بخادم GoldAPI.io. يرجى التحقق من اتصالك بالإنترنت."
            } else {
                "خطأ في الاتصال بـ GoldAPI: ${e.localizedMessage ?: "Unknown network error"}"
            }
            Result.failure(Exception(friendlyError, e))
        }
    }
}
