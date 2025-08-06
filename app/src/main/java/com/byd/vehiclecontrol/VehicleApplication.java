package com.byd.vehiclecontrol;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.byd.vehiclecontrol.shell.CommunicationBinder;

public class VehicleApplication extends Application {
    private static final String TAG = "VehicleApplication";
    public static VehicleApplication INSTANCE;

    // Статическое поле для глобального доступа к Binder (как в Evtech)
    public static IBinder sCommunicationBinder;
    private IBinder communicationBinder;

    private final BroadcastReceiver binderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.byd.vehiclecontrol.ACTION_COMMUNICATION_PROCESS_STARTED".equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    communicationBinder = extras.getBinder("COMMUNICATION_BINDER");
                    if (communicationBinder != null) {
                        // Устанавливаем статическое поле для глобального доступа
                        sCommunicationBinder = communicationBinder;
                        Log.d(TAG, "Received communication binder, set static field");

                        // Уведомляем процесс что binder получен
                        CommunicationBinder.Companion.notifyReceived(communicationBinder);
                    } else {
                        Log.e(TAG, "Binder is null in extras");
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;

        // Регистрируем receiver для получения binder
        IntentFilter filter = new IntentFilter("com.byd.vehiclecontrol.ACTION_COMMUNICATION_PROCESS_STARTED");
        registerReceiver(binderReceiver, filter);

        Log.d(TAG, "Application created, receiver registered");
    }

    public IBinder getCommunicationBinder() {
        return communicationBinder;
    }
    
    // Статический метод для получения Binder (как в Evtech)
    public static IBinder getStaticCommunicationBinder() {
        return sCommunicationBinder;
    }
    
    // Метод для установки Binder из shell процесса
    public static void setCommunicationBinder(IBinder binder) {
        sCommunicationBinder = binder;
        Log.d("VehicleApplication", "Static communication binder set");
    }
}