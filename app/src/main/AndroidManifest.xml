package com.edukit.launcher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class ScreenTimeService extends Service {
    private static final String CHANNEL_ID = "edukit_timer_channel";
    private static final int NOTIFICATION_ID = 101;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tickRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int startSeconds = ConfigManager.getScreenTime(this);
        startForeground(NOTIFICATION_ID, buildNotification(startSeconds));

        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                int current = ConfigManager.getScreenTime(ScreenTimeService.this);

                if (current > 0) {
                    int next = current - 1;
                    ConfigManager.updateScreenTime(ScreenTimeService.this, next);
                    updateNotification(next);
                    handler.postDelayed(this, 1000);
                } else {
                    Intent backToEduKit = new Intent(ScreenTimeService.this, MainActivity.class);
                    backToEduKit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(backToEduKit);

                    stopForeground(true);
                    stopSelf();
                }
            }
        };

        handler.postDelayed(tickRunnable, 1000);
        return START_STICKY;
    }

    private void updateNotification(int secondsLeft) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(secondsLeft));
        }
    }

    private Notification buildNotification(int secondsLeft) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Edu Kit Screen Time")
                .setContentText("Remaining: " + secondsLeft + "s")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
        super.onDestroy();
    }
}
