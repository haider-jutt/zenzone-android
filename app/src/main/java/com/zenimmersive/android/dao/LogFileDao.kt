package com.zenimmersive.android.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zenimmersive.android.model.LogFile


@Dao
interface LogFileDao {
    @Insert
    fun insertLogFile(logFile: LogFile)

    @Query("SELECT * FROM log_file")
    fun getLogFiles(): List<LogFile>

    @Update
    fun updateLogFile(logFile: LogFile)
}