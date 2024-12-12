package com.example.wakebulary.backend

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.util.Locale

class DataBaseClient(context: Context) {
    val helper = DataBaseHelper(context)
    var db: SQLiteDatabase? = null

    init {
        db = helper.writableDatabase
        helper.onUpgrade(db,1,2)
    }

    inline fun <T> withOpenDatabase(block: () -> T): T {
        db = helper.writableDatabase
        try {
            return block()
        } finally {
            db?.close()
        }
    }

    fun insertWord() {
        withOpenDatabase {
            val contentValues = ContentValues()
            contentValues.put(DataBaseVariables.COLUMN_WORD, "easy")
            contentValues.put(DataBaseVariables.COLUMN_CORRECT, 0)
            contentValues.put(DataBaseVariables.COLUMN_ATTEMPTS, 0)
            db?.insert(DataBaseVariables.ENG_TABLE_NAME, null, contentValues)
        }
    }

    fun lowerAndCapitalize(word: String): String =
        word.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }

    fun lowerAndCapitalize(words: List<String>): List<String> {
        return words.map { it ->
            it.lowercase()
                .replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    enum class WordType { RUS, ENGLISH }

    fun getWordType(word: String): WordType? {
        val newWord = lowerAndCapitalize(word)
        val punctuations = "[.,;:!?()\"'-]"
        val rusLetters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        val engLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (newWord.all { it in rusLetters + punctuations }) {
            return WordType.RUS
        } else if (newWord.all { it in engLetters + punctuations }) {
            return WordType.ENGLISH
        }
        return null
    }


    fun checkTranslation() {

    }

    fun insertTranslation() {

    }

    fun eraseTranslation() {

    }

    fun replaceTranslation() {

    }

    fun clearOrphans() {

    }

    fun getStatistics() {

    }

    fun setStatistics() {

    }

    fun translateWord() {

    }

    fun findWord() {

    }

    fun loadList() {

    }
}