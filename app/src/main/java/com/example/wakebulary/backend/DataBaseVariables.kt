package com.example.wakebulary.backend

import android.provider.BaseColumns

object DataBaseVariables {
    const val DATABASE_NAME = "database.db"
    const val DATABASE_VERSION = 1
    const val ENG_TABLE_NAME = "eng"
    const val RUS_TABLE_NAME = "rus"
    const val COLUMN_WORD = "word"
    const val COLUMN_CORRECT = "correct"
    const val COLUMN_ATTEMPTS = "attempts"
    const val ENG_RUS_TABLE_NAME = "eng_rus"
    const val COLUMN_ENG_ID = "eng_id"
    const val COLUMN_RUS_ID = "rus_id"

    const val CREATE_RUS_TABLE = """
        CREATE TABLE $RUS_TABLE_NAME(
            ${BaseColumns._ID} INTEGER PRIMARY KEY,
            $COLUMN_WORD VARCHAR(255) UNIQUE NOT NULL,
            $COLUMN_CORRECT INTEGER DEFAULT 0,
            $COLUMN_ATTEMPTS INTEGER DEFAULT 0
        );
        """

    const val CREATE_ENG_TABLE = """
        CREATE TABLE $ENG_TABLE_NAME(
            ${BaseColumns._ID} INTEGER PRIMARY KEY,
            $COLUMN_WORD VARCHAR(255) UNIQUE NOT NULL,
            $COLUMN_CORRECT INTEGER DEFAULT 0,
            $COLUMN_ATTEMPTS INTEGER DEFAULT 0
        );
        """

    const val CREATE_ENG_RUS_TABLE = """
        CREATE TABLE $ENG_RUS_TABLE_NAME(
            ${BaseColumns._ID} INTEGER PRIMARY KEY,
            $COLUMN_ENG_ID INTEGER NOT NULL,
            $COLUMN_RUS_ID INTEGER NOT NULL,
            FOREIGN KEY($COLUMN_ENG_ID) REFERENCES $ENG_TABLE_NAME(${BaseColumns._ID}) ON DELETE CASCADE,
            FOREIGN KEY($COLUMN_RUS_ID) REFERENCES $RUS_TABLE_NAME(${BaseColumns._ID}) ON DELETE CASCADE,
            UNIQUE ($COLUMN_ENG_ID, $COLUMN_RUS_ID)
        );
        """
}