package com.utar.ucycle;

import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Collections;

public class UcycleMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        if (message.getNotification() == null) return;

        String title = message.getNotification().getTitle();
        String body = message.getNotification().getBody();
        if (title == null) title = "Ucycle";
        if (body == null) return;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, UcycleApplication.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // merge, not update: the user document may not exist yet
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .set(Collections.singletonMap("fcmToken", (Object) token),
                            SetOptions.merge());
        }
    }
}
