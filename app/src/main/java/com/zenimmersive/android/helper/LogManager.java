package com.zenimmersive.android.helper;

import android.content.Context;
import android.util.Log;

import com.zenimmersive.android.AppDatabase;
import com.zenimmersive.android.model.LogFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogManager {
    private Context context;
    private File logFile;
    private static LogManager logManager;
    private BufferedWriter bufferedWriter;


    public static LogManager getLogManager() {
        if (logManager == null) {
            try {
                initLogManager(ContextWrapper.getContext());
            } catch (Exception ignore) {
            }
        }
        return logManager;
    }


    public synchronized static void initLogManager(Context context) {
        if (logManager != null) {
            logManager.flushBuffer();
            logManager.sendLogs();
        }

        File rootFolder = new File(context.getExternalFilesDir(null), "Server");

        if (!rootFolder.exists())
            rootFolder.mkdirs();
        File logManagerFolder = new File(rootFolder, "LogManager");
        if (!logManagerFolder.exists())
            logManagerFolder.mkdirs();

        logManager = new LogManager();
        logManager.logFile = new File(logManagerFolder, KeyStorage.getInstance(context).getLogFileName());
        if (!logManager.logFile.exists()) {
            try {
                logManager.logFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        logManager.context = context;
        logManager.initBuffer();

        logManager.writeLog(getDeviceDetails());
    }

    private static String getDeviceDetails() {
        String s = "Device-infos:";
        s += "\nOS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        s += "\nOS API Level: " + android.os.Build.VERSION.SDK_INT;
        s += "\nDevice: " + android.os.Build.DEVICE;
        s += "\nModel (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")";
        s += "\nRELEASE: " + android.os.Build.VERSION.RELEASE;
        s += "\nBRAND: " + android.os.Build.BRAND;
        s += "\nDISPLAY: " + android.os.Build.DISPLAY;
        s += "\nHARDWARE: " + android.os.Build.HARDWARE;
        s += "\nBuild ID: " + android.os.Build.ID;
        s += "\nMANUFACTURER: " + android.os.Build.MANUFACTURER;
        s += "\nHOST: " + android.os.Build.HOST;
        s += "\nVERSION: 0.3.1";

        return s;
    }

    private void flushBuffer() {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
                bufferedWriter = null;
            }

        } catch (IOException e) {

        }
    }

    StringBuilder stackMessages = null;

    public synchronized void writeLog(final String message) {
        //Log.e(LogManager.class.getSimpleName(), "File Size : " + FileHelper.fileSizeToHumanReadable(logFile.length()) + " ");
        //Log.e(LogManager.class.getSimpleName(), message);
        final String logsMsg = (message + System.getProperty("line.separator") + "Log Time : " + TimeHelper.getUTCDateTime() + " " + System.getProperty("line.separator")).replace("\"", "").replace("'", "");

        if (bufferedWriter == null) {
            if (stackMessages == null)
                stackMessages = new StringBuilder();

            stackMessages.append(logsMsg);
            //Log.e("LogManager", "Log 1 ``" + stackMessages.toString() + "``");
            return;
        } else {
            if (logFile.length() > (1024 * 1024 * 10)) {
                sendLogs();
                return;
            }
            String fullMessage = logsMsg;
            if (stackMessages != null) {
                fullMessage = stackMessages.toString() + logsMsg;
                stackMessages = null;
            }
            try {
                bufferedWriter.write(fullMessage);
                bufferedWriter.flush();
            } catch (IOException e) {

            }
        }
    }


    private void initBuffer() {
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            bufferedWriter = null;
        }
    }


    public synchronized LogFile sendLogs() {
        if (logFile.length() <= 0)
            return null;
        Log.e("LogManager", "LM sendLogs Invoked");
        flushBuffer();

        LogFile logFileData = new LogFile();
        logFileData.setFileName(logFile.getName());
        logFileData.setFilePath(logFile.getPath());
        logFileData.setDateTime(TimeHelper.getDateTime());
        logFileData.setType("LogManager");
        logFileData.setConnectionStatus("success");

        AppDatabase.Companion.getDatabase().logFileDao().insertLogFile(logFileData);
        KeyStorage.getInstance(context).setLogFileName("log_manager_" + System.currentTimeMillis() + ".txt");
        logManager = null;
        initLogManager(context);
        return logFileData;
    }
}
