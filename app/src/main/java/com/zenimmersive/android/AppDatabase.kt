package com.zenimmersive.android

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenimmersive.android.dao.LogFileDao
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.model.LogFile
import java.util.concurrent.Executors

@Database(entities = [LogFile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logFileDao(): LogFileDao

    companion object {
        @Volatile      // used for updated value to all threats
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            synchronized(this)
            {
                return INSTANCE!!
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                //synchronized used for removing multiple instance when we used multiple thread
                synchronized(this) {
                    INSTANCE = buildDatabase(context)
                }
            }
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): AppDatabase? {
            var builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "EvBlocShareDB"
            )
                .allowMainThreadQueries()

            if (false) {
                builder.setQueryCallback(object : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        LogSystem.e("Database", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                    }

                }, Executors.newSingleThreadExecutor())
            }
            return builder.build()
        }

    }
}