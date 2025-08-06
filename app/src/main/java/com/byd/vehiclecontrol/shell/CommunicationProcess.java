package com.byd.vehiclecontrol.shell;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.byd.vehiclecontrol.base.ShellProcess;
import java.lang.reflect.Field;

public class CommunicationProcess extends ShellProcess {
    private static final String TAG = "CommunicationProcess";
    public static final CommunicationProcess INSTANCE = new CommunicationProcess();

    private static final String ACTION_COMMUNICATION_PROCESS_STARTED = "com.byd.vehiclecontrol.ACTION_COMMUNICATION_PROCESS_STARTED";
    private CommunicationBinder communicationBinder;

    private CommunicationProcess() {
        super("VehicleControlProcess");
    }
    
    // Публичный конструктор для использования в VoiceAssistantProcess
    protected CommunicationProcess(String processName) {
        super(processName);
    }

    @Override
    public void execute(String[] args) {
        Log.d(TAG, "CommunicationProcess started");

        // Создаем Binder
        communicationBinder = new CommunicationBinder(this);
        
        // Регистрируем Binder глобально (как в Evtech)
        registerBinderGlobally(communicationBinder);

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
    
    // Регистрация Binder глобально через рефлексию (как в Evtech)
    private void registerBinderGlobally(CommunicationBinder binder) {
        try {
            // Метод 1: Через рефлексию к VehicleApplication
            Class<?> mainAppClass = Class.forName("com.byd.vehiclecontrol.VehicleApplication");
            Field binderField = mainAppClass.getDeclaredField("sCommunicationBinder");
            binderField.setAccessible(true);
            binderField.set(null, binder);
            
            Log.d(TAG, "Binder registered globally via reflection");
            
            // Метод 2: Прямой вызов статического метода (дополнительная надёжность)
            try {
                mainAppClass.getDeclaredMethod("setCommunicationBinder", android.os.IBinder.class)
                          .invoke(null, binder);
                Log.d(TAG, "Binder also set via static method");
            } catch (Exception e2) {
                Log.w(TAG, "Could not call static method, but reflection worked");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register binder globally", e);
            // Попробуем альтернативный способ
            tryAlternativeRegistration(binder);
        }
    }
    
    // Альтернативный способ регистрации через системное свойство
    private void tryAlternativeRegistration(CommunicationBinder binder) {
        try {
            // Устанавливаем системное свойство как индикатор
            android.os.SystemProperties.set("persist.vehicle.binder.ready", "1");
            Log.d(TAG, "Set system property as fallback");
        } catch (Exception e) {
            Log.e(TAG, "All registration methods failed", e);
        }
    }
}