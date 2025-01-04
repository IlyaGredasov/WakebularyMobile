package com.example.wakebulary.backend

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Locale

class DataBaseClient(context: Context) {
    private val helper = DataBaseHelper(context)
    private var db: SQLiteDatabase? = null
    private val dataBasePath: File = context.getDatabasePath("database.db")

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


    private fun getWordType(word: String): WordType {
        val rectifiedWord = lowerAndCapitalize(word)
        if (rectifiedWord.all { it in rusLetters + punctuations }) {
            return WordType.RUS
        } else if (rectifiedWord.all { it in engLetters + punctuations }) {
            return WordType.ENGLISH
        }
        return WordType.INVALID
    }


    private fun checkTypeCompatibility(word: String, translation: List<String>): Boolean {
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

    fun insertTranslation(word: String, translation: List<String>) {
        if (!checkTypeCompatibility(word, translation))
            return

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
            Log.i(
                "Info",
                "Word \"$rectifiedWord\" was inserted into $wordTypeName table successfully."
            )
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

            var cursor: Cursor?
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
                } while (cursor!!.moveToNext())
            }
        }
    }


    fun eraseTranslation(word: String, translation: List<String>) {
        if (!checkTypeCompatibility(word, translation))
            return
        val wordTypeName = getWordType(word).typeName
        val translationsTypeName = getWordType(translation[0]).typeName
        val rectifiedWord = lowerAndCapitalize(word)
        for (transl in translation) {
            val translatedWord = lowerAndCapitalize(transl)
            val query = """
            DELETE FROM eng_rus
            WHERE ${wordTypeName}_id = (
                SELECT id FROM $wordTypeName WHERE word = "$rectifiedWord"
            ) AND ${translationsTypeName}_id = (
                SELECT id FROM $translationsTypeName WHERE word = "$translatedWord"
            )
            """
            withWritableDatabase {
                db?.execSQL(query)
            }
            Log.i("Info", "Translations were deleted successfully, $rectifiedWord, $transl")
            clearOrphans()
        }

    }

    fun replaceTranslation(word: String, previous: String, replacement: String) {
        eraseTranslation(word, listOf(previous))
        insertTranslation(word, listOf(replacement))
    }

    fun clearOrphans() {
        withWritableDatabase {
            db?.execSQL(
                """
            DELETE FROM rus
            WHERE rus.word IN (
                SELECT rus.word
                FROM rus
                        LEFT JOIN eng_rus ON rus.id = eng_rus.rus_id
                WHERE rus_id IS NULL
            )
            """
            )
            db?.execSQL(
                """
            DELETE FROM eng
            WHERE eng.word IN (
                SELECT eng.word
                FROM eng
                        LEFT JOIN eng_rus ON eng.id = eng_rus.eng_id
                WHERE eng_id IS NULL
            )
            """
            )
        }
    }

    fun getStatistics(word: String): Pair<Int, Int> {
        val wordTypeName = getWordType(word).typeName
        if (wordTypeName != "invalid") {
            val query = """
                SELECT correct, attempts
                FROM $wordTypeName
                WHERE ${wordTypeName}.word = "${lowerAndCapitalize(word)}"
                """
            var cursor: Cursor?
            withReadableDatabase {
                cursor = db?.rawQuery(query, null)
                if (!cursor!!.moveToFirst()) {
                    cursor?.close()
                } else {
                    val correctIndex = cursor!!.getColumnIndex("correct")
                    val attemptsIndex = cursor!!.getColumnIndex("attempts")
                    val correct = if (correctIndex >= 0) cursor!!.getInt(correctIndex) else 0
                    val attempts = if (attemptsIndex >= 0) cursor!!.getInt(attemptsIndex) else 0
                    cursor?.close()
                    return Pair(correct, attempts)
                }
            }
        }
        return Pair(0, 0)
    }

    fun setStatistics(word: String, correct: Int, attempts: Int) {
        val wordTypeName = getWordType(word).typeName
        if (wordTypeName != "invalid") {
            val query = """
                UPDATE $wordTypeName
                SET correct = $correct, attempts = $attempts
                WHERE ${wordTypeName}.word = "${lowerAndCapitalize(word)}"
                """
            withWritableDatabase {
                db?.execSQL(query)
            }
            Log.i(
                "Info",
                "Statistics were updated successfully, $word, correct: $correct, attempts: $attempts"
            )
        }
    }

    fun translateWord(word: String): List<String> {
        val rectifiedWord: String = lowerAndCapitalize(word)
        val wordTypeName: String = getWordType(rectifiedWord).typeName
        if (wordTypeName == "invalid")
            return listOf("")
        val oppositeTypeName: String = if (wordTypeName == "eng") "rus" else "eng"
        val query = """
            SELECT ${oppositeTypeName}.word
            FROM
                $wordTypeName
                INNER JOIN eng_rus ON ${wordTypeName}.id = eng_rus.${wordTypeName}_id
                INNER JOIN $oppositeTypeName ON eng_rus.${oppositeTypeName}_id = ${oppositeTypeName}.id
            WHERE ${wordTypeName}.word = "$rectifiedWord"
            """
        var cursor: Cursor?
        withReadableDatabase {
            cursor = db?.rawQuery(query, null)
            if (cursor == null || !cursor!!.moveToFirst()) {
                cursor?.close()
                return listOf("")
            }

            val translations = mutableListOf<String>()

            do {
                val index = cursor!!.getColumnIndex("word")
                if (index >= 0) {
                    val translation = cursor!!.getString(index)
                    translations.add(translation)
                }
            } while (cursor!!.moveToNext())

            cursor?.close()
            return translations
        }

    }

    fun findWord(prefix: String): List<String> {
        val rectifiedPrefix = lowerAndCapitalize(prefix)
        val prefixTypeName = getWordType(rectifiedPrefix).typeName
        if (prefixTypeName == "invalid")
            return emptyList()
        val words = mutableListOf<String>()
        val query = "SELECT word FROM $prefixTypeName WHERE your_column_name LIKE \"$prefix%\""
        val cursor: Cursor? = db?.rawQuery(query, null)
        if (cursor!!.moveToFirst()) {
            do {
                val index = cursor.getColumnIndex("word")
                if (index >= 0) {
                    val word = cursor.getString(index)
                    words.add(word)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return words
    }

    fun loadList(mode: String): MutableList<String> {
        if (mode != "eng" && mode != "rus") {
            return mutableListOf()
        }
        val query = """
            SELECT word
            FROM $mode
            ORDER BY CAST(${mode}.correct AS double)/CAST(MAX(1,${mode}.attempts) AS double);
            """
        var cursor: Cursor?
        withReadableDatabase {
            cursor = db?.rawQuery(query, null)
            if (cursor == null || !cursor!!.moveToFirst()) {
                cursor?.close()
                return mutableListOf()
            }
            val words = mutableListOf<String>()
            do {
                val index = cursor!!.getColumnIndex("word")
                if (index >= 0) {
                    val word = cursor!!.getString(index)
                    words.add(word)
                }
            } while (cursor!!.moveToNext())
            cursor?.close()
            return words
        }
    }

    private fun copyDataBase(existingFile: File, newFile: File) {
        val sourcePath = Paths.get(existingFile.absolutePath)
        val targetPath = Paths.get(newFile.absolutePath)
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun exportDataBase(fileName: String) {
        val dataBaseFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        copyDataBase(dataBasePath.absoluteFile, dataBaseFile)
    }

    //Don't work without permissions
    fun importDataBase(fileName: String) {
        val dataBaseFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        copyDataBase(dataBaseFile, dataBasePath.absoluteFile)
    }
}