package com.example.wakebulary.backend

enum class WordType(val typeName: String) {
    RUS("rus"),
    ENGLISH("eng"),
    INVALID("invalid")
}

const val punctuations = ".,;:!?-"
const val rusLetters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
const val engLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

data class WordTranslation(val word: String, val translation: MutableList<String>)

data class SessionStatistics(var correct: Int, var attempts: Int, var sessionTime: Double) {
    val precision: Double
        get() = if (attempts == 0) 0.0 else correct / attempts.toDouble()

    fun timer() {
        val currentTime = System.currentTimeMillis() / 1000.0
        sessionTime = currentTime - sessionTime
    }
}
