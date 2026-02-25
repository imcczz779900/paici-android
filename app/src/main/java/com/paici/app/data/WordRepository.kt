package com.paici.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WordRepository(context: Context) {

    private val file = File(context.filesDir, "words.json")

    suspend fun readWords(): List<Word> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            val json = JSONArray(file.readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                Word(
                    id        = obj.getInt("id"),
                    english   = obj.getString("english"),
                    chinese   = obj.getString("chinese"),
                    imageUrl  = if (obj.has("imageUrl")) obj.getString("imageUrl") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveWords(words: List<Word>) = withContext(Dispatchers.IO) {
        val json = JSONArray()
        words.forEach { word ->
            json.put(JSONObject().apply {
                put("id",        word.id)
                put("english",   word.english)
                put("chinese",   word.chinese)
                put("createdAt", word.createdAt)
                word.imageUrl?.let { put("imageUrl", it) }
            })
        }
        file.writeText(json.toString())
    }
}
