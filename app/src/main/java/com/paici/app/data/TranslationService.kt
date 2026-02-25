package com.paici.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 使用 MyMemory 免费翻译 API 将英文单词翻译为中文。
 * 无需 API Key，每 IP 每天限额约 5000 字符。
 * 网络异常或找不到结果时返回 "—"。
 */
object TranslationService {

    suspend fun translateToChineseOrDash(english: String): String =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(english, "UTF-8")
                val url = URL(
                    "https://api.mymemory.translated.net/get" +
                        "?q=$encoded&langpair=en|zh-CN"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout    = 5_000
                    requestMethod  = "GET"
                }
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext "—"

                val body = conn.inputStream.bufferedReader().readText()
                JSONObject(body)
                    .getJSONObject("responseData")
                    .getString("translatedText")
                    .trim()
                    .ifBlank { "—" }
            } catch (_: Exception) {
                "—"
            }
        }
}
