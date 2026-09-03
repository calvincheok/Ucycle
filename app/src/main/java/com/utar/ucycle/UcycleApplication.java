package com.utar.ucycle;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class UcycleApplication extends Application {

    public static final String CHANNEL_ID = "ucycle_default";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ucycle notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Borrow requests, due date reminders and chat messages");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
