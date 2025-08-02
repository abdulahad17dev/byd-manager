package com.byd.vehiclecontrol.shell;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.byd.vehiclecontrol.base.ShellProcess;

public class CommunicationProcess extends ShellProcess {
    private static final String TAG = "CommunicationProcess";
    public static final CommunicationProcess INSTANCE = new CommunicationProcess();

    private static final String ACTION_COMMUNICATION_PROCESS_STARTED = "com.byd.vehiclecontrol.ACTION_COMMUNICATION_PROCESS_STARTED";
    private CommunicationBinder communicationBinder;

    private CommunicationProcess() {
        super("VehicleControlProcess");
    }

    @Override
    public void execute(String[] args) {
        Log.d(TAG, "CommunicationProcess started");

        // Создаем Binder
        communicationBinder = new CommunicationBinder(this);

        // Отправляем broadcast
        sendBroadcast();
    }

    private void sendBroadcast() {
        Application currentApplication = ActivityThread.currentApplication();
        Intent intent = new Intent(ACTION_COMMUNICATION_PROCESS_STARTED);
        intent.setPackage("com.byd.vehiclecontrol");

        Log.d(TAG, "Sending broadcast with binder wrapper");

        // Передаем Binder через Bundle с Parcelable wrapper
        Bundle bundle = new Bundle();
        bundle.putBinder("COMMUNICATION_BINDER", communicationBinder);
        intent.putExtras(bundle);

        currentApplication.sendBroadcast(intent);

        // Проверяем через 3 секунды
        handler.postDelayed(() -> {
            if (!CommunicationBinder.isBinderReceived()) {
                Log.e(TAG, "Broadcast timeout - binder not received");
                exitProcess();
            }
        }, 3000);
    }

    public Application getApplication() {
        return this.application;
    }
}