package com.paici.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paici.app.data.TranslationService
import com.paici.app.data.Word
import com.paici.app.data.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WordRepository(application)

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    init {
        viewModelScope.launch {
            _words.value = repository.readWords()
        }
    }

    fun addWord(english: String, chinese: String) {
        val current = _words.value
        if (current.any { it.english.lowercase() == english.lowercase() }) return
        val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val newWord = Word(id = newId, english = english, chinese = chinese)
        val updated = listOf(newWord) + current
        _words.value = updated
        viewModelScope.launch {
            repository.saveWords(updated)
            if (chinese == "—") {
                val translated = TranslationService.translateToChineseOrDash(english)
                if (translated != "—") {
                    val refreshed = _words.value.map {
                        if (it.id == newId) it.copy(chinese = translated) else it
                    }
                    _words.value = refreshed
                    repository.saveWords(refreshed)
                }
            }
        }
    }

    fun updateChinese(id: Int, chinese: String) {
        val updated = _words.value.map { if (it.id == id) it.copy(chinese = chinese) else it }
        _words.value = updated
        viewModelScope.launch {
            repository.saveWords(updated)
        }
    }
}
