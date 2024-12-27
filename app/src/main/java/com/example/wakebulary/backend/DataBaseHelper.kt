package com.example.wakebulary.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DataBaseHelper(context: Context) :
    SQLiteOpenHelper(context, "database.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
            CREATE TABLE rus(
                id INTEGER PRIMARY KEY,
                word VARCHAR(255) UNIQUE NOT NULL,
                correct INTEGER DEFAULT 0,
                attempts INTEGER DEFAULT 0
            )
            """
        )
        Log.i("Info", "Table rus was created")
        db?.execSQL(
            """
            CREATE TABLE eng(
                id INTEGER PRIMARY KEY,
                word VARCHAR(255) UNIQUE NOT NULL,
                correct INTEGER DEFAULT 0,
                attempts INTEGER DEFAULT 0
            )
            """
        )
        Log.i("Info", "Table eng was created")
        db?.execSQL(
            """
            CREATE TABLE eng_rus(
                id INTEGER PRIMARY KEY,
                eng_id INTEGER NOT NULL,
                rus_id INTEGER NOT NULL,
                FOREIGN KEY(eng_id) REFERENCES eng(id) ON DELETE CASCADE,
                FOREIGN KEY(rus_id) REFERENCES rus(id) ON DELETE CASCADE,
                UNIQUE (eng_id, rus_id)
            )
            """
        )
        Log.i("Info", "Table eng_rus was created")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS rus")
        db?.execSQL("DROP TABLE IF EXISTS eng")
        db?.execSQL("DROP TABLE IF EXISTS eng_rus")
        onCreate(db)
    }
}