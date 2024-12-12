package com.example.wakebulary.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DataBaseHelper(context: Context) :
    SQLiteOpenHelper(context, DataBaseVariables.DATABASE_NAME, null, DataBaseVariables.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(DataBaseVariables.CREATE_RUS_TABLE)
        Log.i("Info", "Database rus was created")
        db?.execSQL(DataBaseVariables.CREATE_ENG_TABLE)
        Log.i("Info", "Database eng was created")
        db?.execSQL(DataBaseVariables.CREATE_ENG_RUS_TABLE)
        Log.i("Info", "Database eng_rus was created")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${DataBaseVariables.RUS_TABLE_NAME}")
        db?.execSQL("DROP TABLE IF EXISTS ${DataBaseVariables.ENG_TABLE_NAME}")
        db?.execSQL("DROP TABLE IF EXISTS ${DataBaseVariables.ENG_RUS_TABLE_NAME}")
        onCreate(db)
    }
}