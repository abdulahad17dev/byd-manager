package com.byd.vehiclecontrol.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.byd.vehiclecontrol.shell.CommunicationProcess;

public class ShellService extends Service {
    private static final String TAG = "ShellService";
    private HandlerThread shellThread;
    private Handler shellHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting communication process...");

        shellThread = new HandlerThread("CommunicationProcessThread");
        shellThread.start();

        shellHandler = new Handler(shellThread.getLooper());

        shellHandler.post(() -> {
            try {
                // Запускаем CommunicationProcess как в AzerVoice
                CommunicationProcess.INSTANCE.run(new String[]{});
            } catch (Exception e) {
                Log.e(TAG, "Communication process failed", e);
            }
        });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (shellThread != null) {
            shellThread.quitSafely();
        }
    }
}