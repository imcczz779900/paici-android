package com.paici.app.data

data class Word(
    val id: Int = 0,
    val english: String,
    val chinese: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
