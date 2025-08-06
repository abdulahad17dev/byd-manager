package com.byd.vehiclecontrol;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.byd.vehiclecontrol.IVehicleControl;

/**
 * Service для предоставления Binder между процессами
 */
public class VehicleBinderService extends Service {
    private static final String TAG = "VehicleBinderService";
    
    // Статический Binder, который будет установлен из shell процесса
    public static IBinder sShellBinder;
    
    // Метод для получения Shell Binder
    private static IBinder getShellBinder() {
        // Сначала проверяем статическое поле
        if (sShellBinder != null && sShellBinder.isBinderAlive()) {
            Log.d(TAG, "Используем статический sShellBinder");
            return sShellBinder;
        }
        
        // Пытаемся получить из глобального экземпляра CommunicationBinder
        try {
            IBinder globalBinder = com.byd.vehiclecontrol.shell.CommunicationBinder.getGlobalInstance();
            if (globalBinder != null && globalBinder.isBinderAlive()) {
                Log.d(TAG, "✅ Получен Binder из CommunicationBinder.globalInstance");
                sShellBinder = globalBinder;
                return globalBinder;
            }
        } catch (Exception e) {
            Log.d(TAG, "Ошибка получения globalInstance: " + e.getMessage());
        }
        
        // Пытаемся получить из системного сервиса (может не работать без прав)
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getServiceMethod = serviceManagerClass.getDeclaredMethod(
                    "getService", String.class);
            getServiceMethod.setAccessible(true);
            IBinder binder = (IBinder) getServiceMethod.invoke(null, "byd.vehicle.control");
            if (binder != null && binder.isBinderAlive()) {
                Log.d(TAG, "✅ Получен Binder из системного сервиса byd.vehicle.control");
                sShellBinder = binder;
                return binder;
            }
        } catch (Exception e) {
            Log.d(TAG, "Не удалось получить системный сервис: " + e.getMessage());
        }
        
        return null;
    }
    
    private final IBinder mBinder = new IVehicleControl.Stub() {
        @Override
        public int sendCommand(int deviceType, int eventType, int value) throws RemoteException {
            Log.d(TAG, String.format("sendCommand: device=%d, event=%d, value=%d", 
                    deviceType, eventType, value));
            
            // Пытаемся получить Binder различными способами
            IBinder binder = getShellBinder();
            Log.d(TAG, "Shell binder status: " + (binder != null ? "available" : "null"));
            
            // Если есть shell binder, перенаправляем команду
            if (binder != null) {
                try {
                    Log.d(TAG, "Calling CommunicationBinder.setInt...");
                    // Используем CommunicationBinder.Companion для вызова
                    int result = com.byd.vehiclecontrol.shell.CommunicationBinder.Companion
                            .setInt(binder, deviceType, eventType, value);
                    Log.d(TAG, "CommunicationBinder.setInt result: " + result);
                    return result;
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка вызова shell binder", e);
                    return -1;
                }
            }
            
            Log.w(TAG, "Shell binder не доступен");
            return -1;
        }
        
        @Override
        public boolean isConnected() throws RemoteException {
            return sShellBinder != null && sShellBinder.isBinderAlive();
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service создан");
        Log.d(TAG, "Initial sShellBinder status: " + (sShellBinder != null));
        
        // Проверяем статус Binder периодически каждые 2 секунды
        final android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Пытаемся получить Binder если его еще нет
                if (sShellBinder == null || !sShellBinder.isBinderAlive()) {
                    IBinder binder = getShellBinder();
                    if (binder != null) {
                        Log.d(TAG, "✅ Shell Binder получен через периодическую проверку!");
                    } else {
                        Log.d(TAG, "⏳ Shell Binder еще не доступен, проверим еще раз через 2 секунды");
                        handler.postDelayed(this, 2000); // Проверяем снова через 2 секунды
                    }
                } else {
                    Log.d(TAG, "✅ Shell Binder активен и готов к работе!");
                }
            }
        }, 2000);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service привязан");
        return mBinder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service уничтожен");
    }
    
    /**
     * Устанавливает Binder из shell процесса
     */
    public static void setShellBinder(IBinder binder) {
        sShellBinder = binder;
        Log.d(TAG, "Shell binder установлен: " + (binder != null));
        Log.d(TAG, "Shell binder class: " + (binder != null ? binder.getClass().getName() : "null"));
        Log.d(TAG, "Shell binder alive: " + (binder != null ? binder.isBinderAlive() : "false"));
    }
}