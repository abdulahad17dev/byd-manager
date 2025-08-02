package com.byd.vehiclecontrol;

import android.util.Log;

/**
 * API для управления автомобилем через BYDAutoManager
 * Предоставляет высокоуровневые методы для управления функциями автомобиля
 */
public class VehicleControlAPI {
    private static final String TAG = "VehicleControlAPI";

    public Object autoManager;

    public VehicleControlAPI(Object autoManager) {
        this.autoManager = autoManager;
        Log.d(TAG, "VehicleControlAPI инициализирован с: " + autoManager.getClass().getName());
    }

    /**
     * Универсальный метод для установки целочисленных значений
     */
    public boolean setInt(int deviceType, int eventType, int value) {
        try {
            Log.d(TAG, String.format("setInt(%d, %d, %d)", deviceType, eventType, value));

            java.lang.reflect.Method setIntMethod = autoManager.getClass()
                    .getMethod("setInt", int.class, int.class, int.class);

            Object result = setIntMethod.invoke(autoManager, deviceType, eventType, value);

            Log.d(TAG, "setInt результат: " + result);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка setInt: " + Log.getStackTraceString(e));
            return false;
        }
    }

    /**
     * Универсальный метод для получения целочисленных значений
     */
    public int getInt(int deviceType, int eventType) {
        try {
            Log.d(TAG, String.format("getInt(%d, %d)", deviceType, eventType));

            java.lang.reflect.Method getIntMethod = autoManager.getClass()
                    .getMethod("getInt", int.class, int.class);

            Object result = getIntMethod.invoke(autoManager, deviceType, eventType);

            int value = (Integer) result;
            Log.d(TAG, "getInt результат: " + value);
            return value;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка getInt: " + Log.getStackTraceString(e));
            return -1;
        }
    }

    // ===== МЕТОДЫ УПРАВЛЕНИЯ ОКНАМИ =====

    /**
     * Управление окнами автомобиля
     * @param window 1=передние, 2=задние, 3=левые, 4=правые
     * @param action 0=закрыть, 1=открыть, 2=стоп
     */
    public boolean controlWindow(int window, int action) {
        Log.d(TAG, String.format("Управление окном %d, действие %d", window, action));

        // Пробуем разные комбинации параметров для окон
        int[] windowDeviceTypes = {1, 2, 10, 20, 100}; // Возможные типы устройств для окон
        int[] windowEventTypes = {1, 2, 3, 10, 11, 12}; // Возможные типы событий

        for (int deviceType : windowDeviceTypes) {
            for (int eventType : windowEventTypes) {
                try {
                    if (setInt(deviceType + window, eventType, action)) {
                        Log.d(TAG, String.format("✅ Окно управляется через deviceType=%d, eventType=%d",
                                deviceType + window, eventType));
                        return true;
                    }
                } catch (Exception e) {
                    // Продолжаем поиск
                }
            }
        }

        Log.w(TAG, "Не удалось найти параметры для управления окном");
        return false;
    }

    /**
     * Открыть переднее левое окно
     */
    public boolean openFrontLeftWindow() {
        return controlWindow(1, 1);
    }

    /**
     * Закрыть переднее левое окно
     */
    public boolean closeFrontLeftWindow() {
        return controlWindow(1, 0);
    }

    /**
     * Открыть все окна
     */
    public boolean openAllWindows() {
        boolean success = true;
        success &= controlWindow(1, 1); // Переднее левое
        success &= controlWindow(2, 1); // Переднее правое
        success &= controlWindow(3, 1); // Заднее левое
        success &= controlWindow(4, 1); // Заднее правое
        return success;
    }

    /**
     * Закрыть все окна
     */
    public boolean closeAllWindows() {
        boolean success = true;
        success &= controlWindow(1, 0); // Переднее левое
        success &= controlWindow(2, 0); // Переднее правое
        success &= controlWindow(3, 0); // Заднее левое
        success &= controlWindow(4, 0); // Заднее правое
        return success;
    }

    // ===== МЕТОДЫ УПРАВЛЕНИЯ ДВЕРЬМИ =====

    /**
     * Управление дверьми
     * @param door 1=передняя левая, 2=передняя правая, 3=задняя левая, 4=задняя правая, 5=багажник
     * @param action 0=закрыть/заблокировать, 1=открыть/разблокировать
     */
    public boolean controlDoor(int door, int action) {
        Log.d(TAG, String.format("Управление дверью %d, действие %d", door, action));

        // Пробуем параметры для дверей
        int[] doorDeviceTypes = {5, 6, 50, 60, 200};
        int[] doorEventTypes = {1, 2, 5, 6, 20, 21};

        for (int deviceType : doorDeviceTypes) {
            for (int eventType : doorEventTypes) {
                try {
                    if (setInt(deviceType + door, eventType, action)) {
                        Log.d(TAG, String.format("✅ Дверь управляется через deviceType=%d, eventType=%d",
                                deviceType + door, eventType));
                        return true;
                    }
                } catch (Exception e) {
                    // Продолжаем поиск
                }
            }
        }

        return false;
    }

    /**
     * Разблокировать все двери
     */
    public boolean unlockAllDoors() {
        boolean success = true;
        for (int door = 1; door <= 5; door++) {
            success &= controlDoor(door, 1);
        }
        return success;
    }

    /**
     * Заблокировать все двери
     */
    public boolean lockAllDoors() {
        boolean success = true;
        for (int door = 1; door <= 5; door++) {
            success &= controlDoor(door, 0);
        }
        return success;
    }

    // ===== МЕТОДЫ ПОИСКА ПАРАМЕТРОВ =====

    /**
     * Автоматический поиск рабочих параметров для функций автомобиля
     */
    public void discoverVehicleFunctions() {
        Log.d(TAG, "Начинаем поиск доступных функций автомобиля...");

        // Диапазоны для поиска
        int[] deviceTypes = {1, 2, 3, 4, 5, 10, 20, 50, 100, 200};
        int[] eventTypes = {1, 2, 3, 4, 5, 10, 11, 12, 20, 21, 22};
        int[] testValues = {0, 1};

        int foundCount = 0;

        for (int deviceType : deviceTypes) {
            for (int eventType : eventTypes) {
                for (int testValue : testValues) {
                    try {
                        // Сначала получаем текущее значение
                        int currentValue = getInt(deviceType, eventType);

                        // Пытаемся установить новое значение
                        if (setInt(deviceType, eventType, testValue)) {

                            // Проверяем, изменилось ли значение
                            int newValue = getInt(deviceType, eventType);

                            if (newValue != currentValue) {
                                Log.d(TAG, String.format("🎯 НАЙДЕНА ФУНКЦИЯ: deviceType=%d, eventType=%d " +
                                                "(было: %d, стало: %d)",
                                        deviceType, eventType, currentValue, newValue));
                                foundCount++;

                                // Возвращаем обратно если удалось изменить
                                setInt(deviceType, eventType, currentValue);
                            }
                        }

                        // Небольшая пауза чтобы не перегружать систему
                        Thread.sleep(10);

                    } catch (Exception e) {
                        // Игнорируем ошибки при поиске
                    }
                }
            }
        }

        Log.d(TAG, "Поиск завершен. Найдено " + foundCount + " доступных функций.");
    }

    /**
     * Тестирует конкретную функцию
     */
    public void testFunction(int deviceType, int eventType, String functionName) {
        Log.d(TAG, "Тестирование функции: " + functionName);

        try {
            // Получаем текущее состояние
            int currentValue = getInt(deviceType, eventType);
            Log.d(TAG, String.format("%s - текущее значение: %d", functionName, currentValue));

            // Пытаемся изменить
            boolean success1 = setInt(deviceType, eventType, currentValue == 0 ? 1 : 0);
            Thread.sleep(100);
            int newValue1 = getInt(deviceType, eventType);

            // Возвращаем обратно
            boolean success2 = setInt(deviceType, eventType, currentValue);
            Thread.sleep(100);
            int newValue2 = getInt(deviceType, eventType);

            Log.d(TAG, String.format("%s - тест: %d → %d → %d (успех: %s/%s)",
                    functionName, currentValue, newValue1, newValue2, success1, success2));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка тестирования " + functionName + ": " + e.getMessage());
        }
    }
}