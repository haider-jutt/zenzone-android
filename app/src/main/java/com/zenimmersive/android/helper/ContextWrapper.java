package com.zenimmersive.android.helper;

import android.content.Context;

import com.zenimmersive.android.AppDatabase;

public class ContextWrapper {
    private Context context;

    private static ContextWrapper contextWrapper;

    public static void saveContext(Context context) {
        contextWrapper = new ContextWrapper();
        contextWrapper.context = context;
        AppDatabase.Companion.getDatabase(context);
        LogManager.initLogManager(context);
        KeyStorage.getInstance(context);
    }

    public static Context getContext() {
        return contextWrapper.requiredContext();
    }

    public Context requiredContext() {
        return context;
    }
}
