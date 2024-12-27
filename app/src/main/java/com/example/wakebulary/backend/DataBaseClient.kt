package com.example.wakebulary.backend

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.Locale

class DataBaseClient(context: Context) {
    private val helper = DataBaseHelper(context)
    private var db: SQLiteDatabase? = null


    private inline fun <T> withWritableDatabase(execute: () -> T): T {
        db = helper.writableDatabase
        try {
            return execute()
        } finally {
            db?.close()
        }
    }

    private inline fun <T> withReadableDatabase(execute: () -> T): T {
        db = helper.readableDatabase
        try {
            return execute()
        } finally {
            db?.close()
        }
    }

    init {
        withWritableDatabase {
            helper.onUpgrade(db, 1, 1)
        }
    }

    private fun lowerAndCapitalize(word: String): String =
        word.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }

    private fun lowerAndCapitalize(words: List<String>): List<String> {
        return words.map { it ->
            it.lowercase()
                .replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }


    private fun getWordType(word: String): WordType {
        val rectifiedWord = lowerAndCapitalize(word)
        if (rectifiedWord.all { it in rusLetters + punctuations }) {
            return WordType.RUS
        } else if (rectifiedWord.all { it in engLetters + punctuations }) {
            return WordType.ENGLISH
        }
        return WordType.INVALID
    }


    fun checkTranslation(word: String, translation: List<String>): Boolean {
        val wordType = getWordType(word)
        val translationTypes = translation.map { getWordType(it) }
        if (wordType == WordType.INVALID || translationTypes.any { it == WordType.INVALID }) {
            Log.i(
                "Info",
                "Invalid word type in the query, $word, ${translation.joinToString(", ")}"
            )
            return false
        }
        if (translationTypes.toSet().size > 1) {
            Log.i(
                "Info",
                "Different word types in the translations, $word, ${translation.joinToString(", ")}"
            )
            return false
        }
        if (wordType == translationTypes.first()) {
            Log.i("Info", "Required word translations, $word, ${translation.joinToString(", ")}")
            return false
        }
        return true
    }

    fun insertTranslation(word: String, translation: List<String>) {
        if (!checkTranslation(word, translation)) return

        val wordTypeName = getWordType(word).typeName
        val translationsTypeName = getWordType(translation[0]).typeName
        val rectifiedWord = lowerAndCapitalize(word)

        if (!isWordInDataBase(rectifiedWord)) {
            withWritableDatabase {
                db?.execSQL(
                    """
                    INSERT INTO $wordTypeName (word) VALUES ("$rectifiedWord")
                    """
                )
            }
            Log.i("Info", "Word \"$rectifiedWord\" was inserted into $wordTypeName table successfully.")
        }

        for (transl in translation) {
            val translatedWord = lowerAndCapitalize(transl)
            if (!isWordInDataBase(translatedWord)) {
                withWritableDatabase {
                    db?.execSQL(
                        """
                        INSERT INTO $translationsTypeName (word) VALUES ("$translatedWord")
                        """
                    )
                }
                Log.i(
                    "Info",
                    "Word \"$translatedWord\" was inserted into $translationsTypeName table successfully."
                )
            }

            val query = """
            SELECT ${wordTypeName}.id as ${wordTypeName}_id, ${translationsTypeName}.id as ${translationsTypeName}_id
            FROM $wordTypeName
            INNER JOIN eng_rus ON ${wordTypeName}.id = eng_rus.${wordTypeName}_id
            INNER JOIN $translationsTypeName ON eng_rus.${translationsTypeName}_id = ${translationsTypeName}.id
            WHERE ${wordTypeName}.word = "$rectifiedWord" AND ${translationsTypeName}.word = "$translatedWord"
            """

            var cursor: Cursor? = null
            withReadableDatabase {
                do {
                    cursor = db?.rawQuery(query, null)

                    if (cursor != null && cursor!!.moveToFirst()) {
                        cursor?.close()
                    } else {

                        db?.execSQL(
                            """
                    INSERT INTO eng_rus (${wordTypeName}_id, ${translationsTypeName}_id)
                    SELECT ${wordTypeName}.id, ${translationsTypeName}.id
                    FROM $wordTypeName
                    CROSS JOIN $translationsTypeName
                    WHERE ${wordTypeName}.word = "$rectifiedWord" AND ${translationsTypeName}.word = "$translatedWord"
                    """
                        )
                    }
                    Log.i(
                        "Info",
                        "Translations were inserted successfully, $rectifiedWord, $translatedWord"
                    )
                } while(cursor!!.moveToNext())
            }
        }
    }


    fun eraseTranslation(word: String, translation: List<String>) {

    }

    fun replaceTranslation(word: String, previous: String, replacement: String) {

    }

    fun clearOrphans() {

    }

    fun getStatistics(word: String): Pair<Int, Int> {
        return Pair(0, 0)
    }

    fun setStatistics(word: String, correct: Int, attempts: Int) {

    }

    fun translateWord(word: String): List<String> {
        val rectifiedWord: String = lowerAndCapitalize(word)
        val wordTypeName: String = getWordType(rectifiedWord).typeName
        if (wordTypeName == "invalid")
            return listOf("")
        val oppositeTypeName: String = if (wordTypeName == "eng") "rus" else "eng";
        val query = """
            SELECT ${oppositeTypeName}.word
            FROM
                $wordTypeName
                INNER JOIN eng_rus ON ${wordTypeName}.id = eng_rus.${wordTypeName}_id
                INNER JOIN $oppositeTypeName ON eng_rus.${oppositeTypeName}_id = ${oppositeTypeName}.id
            WHERE ${wordTypeName}.word = "$rectifiedWord"
            """
        var cursor: Cursor? = null
        withReadableDatabase {
            cursor = db?.rawQuery(query, null)


            if (cursor == null || !cursor!!.moveToFirst()) {
                cursor?.close()
                return listOf("")
            }

            val translations = mutableListOf<String>()

            do {
                val index = cursor!!.getColumnIndex("word")
                val translation = if (index >= 0) cursor!!.getString(index) else ""
                translations.add(translation)
            } while (cursor!!.moveToNext())

            cursor?.close()
            return translations
        }

    }

    fun isWordInDataBase(word: String): Boolean {
        val wordType = getWordType(word).typeName
        if (wordType == "invalid") {
            Log.i("Info", "Invalid word type, $word")
            return false
        }
        val query = """
        SELECT ${wordType}.word
        FROM $wordType
        WHERE word = "${lowerAndCapitalize(word)}"
        """
        withReadableDatabase {
            val cursor = db?.rawQuery(query, null)
            return cursor?.use { it.moveToFirst() } ?: false
        }
    }

    fun findWord(word: String) {

    }

    fun loadList(): List<String> {
        return listOf()
    }

    fun importDataBase() {

    }

    fun exportDataBase() {

    }
}