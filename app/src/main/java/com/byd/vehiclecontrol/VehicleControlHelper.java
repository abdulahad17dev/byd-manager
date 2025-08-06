package com.byd.vehiclecontrol;

import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

/**
 * Хелпер для отправки команд управления автомобилем через межпроцессное взаимодействие
 * Использует Binder для связи с shell процессом, который имеет системные права
 */
public class VehicleControlHelper {
    private static final String TAG = "VehicleControlHelper";
    
    // Коды транзакций (должны совпадать с CommunicationBinder)
    private static final int TRANSACTION_SET_INT = 20;
    private static final int TRANSACTION_START_VEHICLE = 21;
    private static final int TRANSACTION_NOTIFY_RECEIVED = 7;
    
    // Дескриптор интерфейса
    private static final String DESCRIPTOR = "VoiceAssistant";
    
    /**
     * Получить Binder для взаимодействия с shell процессом
     */
    private static IBinder getCommunicationBinder() {
        // Пробуем получить статический Binder
        IBinder binder = VehicleApplication.getStaticCommunicationBinder();
        
        if (binder == null) {
            Log.w(TAG, "Static binder not available, trying instance method");
            // Альтернативно через инстанс
            if (VehicleApplication.INSTANCE != null) {
                binder = VehicleApplication.INSTANCE.getCommunicationBinder();
            }
        }
        
        if (binder == null) {
            Log.e(TAG, "❌ Communication binder not available! Shell process not running?");
        } else {
            Log.d(TAG, "✅ Communication binder obtained");
        }
        
        return binder;
    }
    
    /**
     * Проверить доступность связи с автомобилем
     */
    public static boolean isVehicleConnectionAvailable() {
        IBinder binder = getCommunicationBinder();
        if (binder == null) {
            return false;
        }
        
        // Проверяем, что Binder активен
        try {
            return binder.isBinderAlive() && binder.pingBinder();
        } catch (Exception e) {
            Log.e(TAG, "Binder ping failed", e);
            return false;
        }
    }
    
    /**
     * Отправить команду setInt в автомобиль
     * @param deviceType Тип устройства
     * @param eventType Тип события
     * @param value Значение
     * @return Результат операции или -1 при ошибке
     */
    public static int sendSetIntCommand(int deviceType, int eventType, int value) {
        IBinder binder = getCommunicationBinder();
        if (binder == null) {
            Log.e(TAG, "Cannot send command: binder not available");
            return -1;
        }
        
        Parcel data = null;
        Parcel reply = null;
        
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            
            // Записываем дескриптор интерфейса
            data.writeInterfaceToken(DESCRIPTOR);
            
            // Записываем параметры
            data.writeInt(deviceType);
            data.writeInt(eventType);
            data.writeInt(value);
            
            Log.d(TAG, String.format("Sending setInt command: device=%d, event=%d, value=%d", 
                    deviceType, eventType, value));
            
            // Выполняем транзакцию
            boolean success = binder.transact(TRANSACTION_SET_INT, data, reply, 0);
            
            if (!success) {
                Log.e(TAG, "Transaction failed");
                return -1;
            }
            
            // Читаем результат
            reply.readException(); // Проверяем на исключения
            int result = reply.readInt();
            
            Log.d(TAG, String.format("setInt command result: %d", result));
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send setInt command", e);
            return -1;
        } finally {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        }
    }
    
    /**
     * Отправить команду startVehicle
     * @param deviceType Тип устройства
     * @param time Время
     * @param reason Причина
     * @param reasonStr Строковое описание
     * @return Результат операции или -1 при ошибке
     */
    public static int sendStartVehicleCommand(int deviceType, long time, int reason, String reasonStr) {
        IBinder binder = getCommunicationBinder();
        if (binder == null) {
            Log.e(TAG, "Cannot send startVehicle: binder not available");
            return -1;
        }
        
        Parcel data = null;
        Parcel reply = null;
        
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(deviceType);
            data.writeLong(time);
            data.writeInt(reason);
            data.writeString(reasonStr);
            
            Log.d(TAG, String.format("Sending startVehicle: device=%d, time=%d, reason=%d, str=%s", 
                    deviceType, time, reason, reasonStr));
            
            boolean success = binder.transact(TRANSACTION_START_VEHICLE, data, reply, 0);
            
            if (!success) {
                Log.e(TAG, "startVehicle transaction failed");
                return -1;
            }
            
            reply.readException();
            int result = reply.readInt();
            
            Log.d(TAG, String.format("startVehicle result: %d", result));
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send startVehicle command", e);
            return -1;
        } finally {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        }
    }
    
    // ===== УДОБНЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ АВТОМОБИЛЕМ =====
    
    /**
     * Управление окнами
     * @param windowPosition 1=переднее левое, 2=переднее правое, 3=заднее левое, 4=заднее правое
     * @param action 0=закрыть, 1=открыть
     */
    public static boolean controlWindow(int windowPosition, int action) {
        Log.d(TAG, String.format("Управление окном %d, действие: %s", 
                windowPosition, action == 1 ? "открыть" : "закрыть"));
        
        // Пробуем разные комбинации параметров для окон (на основе анализа Evtech)
        int[][] windowParams = {
            {1, 2},   // deviceType=1, eventType=2 (как в FloatingRecordButtonService)
            {2, 1},   // deviceType=2, eventType=1
            {10, 11}, // deviceType=10, eventType=11
            {1, 10},  // deviceType=1, eventType=10
            {windowPosition, 2}, // позиция окна как deviceType
            {windowPosition + 10, 2}, // смещенная позиция
        };
        
        for (int[] params : windowParams) {
            int deviceType = params[0];
            int eventType = params[1];
            
            int result = sendSetIntCommand(deviceType, eventType, action);
            if (result >= 0) {
                Log.d(TAG, String.format("✅ Окно %d управляется через deviceType=%d, eventType=%d", 
                        windowPosition, deviceType, eventType));
                return true;
            }
        }
        
        Log.w(TAG, "❌ Не удалось управлять окном " + windowPosition);
        return false;
    }
    
    /**
     * Открыть переднее левое окно
     */
    public static boolean openFrontLeftWindow() {
        return controlWindow(1, 1);
    }
    
    /**
     * Закрыть переднее левое окно
     */
    public static boolean closeFrontLeftWindow() {
        return controlWindow(1, 0);
    }
    
    /**
     * Открыть все окна
     */
    public static boolean openAllWindows() {
        Log.d(TAG, "Открываем все окна...");
        boolean success = true;
        for (int i = 1; i <= 4; i++) {
            success &= controlWindow(i, 1);
        }
        return success;
    }
    
    /**
     * Закрыть все окна
     */
    public static boolean closeAllWindows() {
        Log.d(TAG, "Закрываем все окна...");
        boolean success = true;
        for (int i = 1; i <= 4; i++) {
            success &= controlWindow(i, 0);
        }
        return success;
    }
    
    /**
     * Тест связи - отправляет простую команду для проверки
     */
    public static boolean testConnection() {
        Log.d(TAG, "Тестируем связь с автомобилем...");
        
        // Отправляем безопасную тестовую команду
        int result = sendSetIntCommand(999, 999, 0);
        
        if (result >= 0) {
            Log.d(TAG, "✅ Связь с автомобилем работает (result: " + result + ")");
            return true;
        } else {
            Log.e(TAG, "❌ Связь с автомобилем не работает");
            return false;
        }
    }
    
    /**
     * Автопоиск рабочих параметров для функций автомобиля
     */
    public static void discoverVehicleFunctions() {
        Log.d(TAG, "🔍 Запуск автопоиска функций автомобиля...");
        
        if (!isVehicleConnectionAvailable()) {
            Log.e(TAG, "❌ Связь недоступна, поиск невозможен");
            return;
        }
        
        // Диапазоны для поиска (на основе анализа команд из Evtech)
        int[] deviceTypes = {1, 2, 3, 4, 5, 10, 11, 12, 20, 21, 22, 50, 100};
        int[] eventTypes = {1, 2, 3, 10, 11, 12, 20, 21, 22};
        
        int foundCount = 0;
        
        for (int deviceType : deviceTypes) {
            for (int eventType : eventTypes) {
                try {
                    // Пробуем отправить команду с безопасным значением
                    int result = sendSetIntCommand(deviceType, eventType, 0);
                    
                    if (result >= 0) {
                        Log.d(TAG, String.format("🎯 Найдена функция: deviceType=%d, eventType=%d (result=%d)", 
                                deviceType, eventType, result));
                        foundCount++;
                    }
                    
                    // Пауза чтобы не перегружать систему
                    Thread.sleep(50);
                    
                } catch (Exception e) {
                    // Игнорируем ошибки
                }
            }
        }
        
        Log.d(TAG, String.format("🏁 Поиск завершен. Найдено %d потенциально рабочих функций", foundCount));
    }
}