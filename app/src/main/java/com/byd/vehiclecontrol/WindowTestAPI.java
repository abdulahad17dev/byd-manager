package com.byd.vehiclecontrol;

import android.util.Log;

/**
 * Специальный тест для окна с конкретными параметрами
 */
public class WindowTestAPI {
    private static final String TAG = "WindowTestAPI";

    // Конкретные параметры для тестирования
    private static final int WINDOW_SYSTEM = 1001;
    private static final int WINDOW_EVENT = 1125122107;
    private static final int WINDOW_OPEN = 1;
    private static final int WINDOW_CLOSE = 0;

    private Object autoManager;

    public WindowTestAPI(Object autoManager) {
        this.autoManager = autoManager;
        Log.d(TAG, "WindowTestAPI инициализирован для тестирования окна");
        Log.d(TAG, "Параметры: system=" + WINDOW_SYSTEM + ", event=" + WINDOW_EVENT);
    }

    /**
     * Открыть окно с заданными параметрами
     */
    public boolean openWindow() {
        return setWindowState(WINDOW_OPEN);
    }

    /**
     * Закрыть окно с заданными параметрами
     */
    public boolean closeWindow() {
        return setWindowState(WINDOW_CLOSE);
    }

    /**
     * Установить состояние окна
     */
    private boolean setWindowState(int state) {
        try {
            Log.d(TAG, String.format("Установка состояния окна: setInt(%d, %d, %d)",
                    WINDOW_SYSTEM, WINDOW_EVENT, state));

            // Получаем метод setInt
            java.lang.reflect.Method setIntMethod = autoManager.getClass()
                    .getMethod("setInt", int.class, int.class, int.class);

            // Вызываем метод
            Object result = setIntMethod.invoke(autoManager, WINDOW_SYSTEM, WINDOW_EVENT, state);

            Log.d(TAG, "✅ setInt выполнен, результат: " + result);

            // Проверяем результат через небольшую паузу
            Thread.sleep(500);
            int currentState = getWindowState();

            if (currentState == state) {
                Log.d(TAG, "🎯 УСПЕХ! Окно переведено в состояние: " + state);
                return true;
            } else {
                Log.w(TAG, "⚠️ Состояние не изменилось. Ожидали: " + state + ", получили: " + currentState);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка управления окном: " + Log.getStackTraceString(e));
            return false;
        }
    }

    /**
     * Получить текущее состояние окна
     */
    public int getWindowState() {
        try {
            Log.d(TAG, String.format("Получение состояния окна: getInt(%d, %d)",
                    WINDOW_SYSTEM, WINDOW_EVENT));

            // Получаем метод getInt
            java.lang.reflect.Method getIntMethod = autoManager.getClass()
                    .getMethod("getInt", int.class, int.class);

            // Вызываем метод
            Object result = getIntMethod.invoke(autoManager, WINDOW_SYSTEM, WINDOW_EVENT);

            int state = (Integer) result;
            Log.d(TAG, "Текущее состояние окна: " + state);
            return state;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения состояния окна: " + Log.getStackTraceString(e));
            return -1;
        }
    }

    /**
     * Полный тест функциональности окна
     */
    public void runFullWindowTest() {
        Log.d(TAG, "🚗 === ЗАПУСК ПОЛНОГО ТЕСТА ОКНА ===");

        try {
            // Шаг 1: Получаем текущее состояние
            Log.d(TAG, "Шаг 1: Проверка текущего состояния");
            int initialState = getWindowState();
            Log.d(TAG, "Начальное состояние окна: " + initialState);

            Thread.sleep(1000);

            // Шаг 2: Пробуем открыть окно
            Log.d(TAG, "Шаг 2: Попытка открыть окно");
            boolean openResult = openWindow();
            Log.d(TAG, "Результат открытия: " + (openResult ? "УСПЕХ" : "НЕУДАЧА"));

            Thread.sleep(2000); // Даем время на выполнение

            // Шаг 3: Проверяем состояние после открытия
            Log.d(TAG, "Шаг 3: Проверка состояния после открытия");
            int openState = getWindowState();
            Log.d(TAG, "Состояние после открытия: " + openState);

            Thread.sleep(1000);

            // Шаг 4: Пробуем закрыть окно
            Log.d(TAG, "Шаг 4: Попытка закрыть окно");
            boolean closeResult = closeWindow();
            Log.d(TAG, "Результат закрытия: " + (closeResult ? "УСПЕХ" : "НЕУДАЧА"));

            Thread.sleep(2000); // Даем время на выполнение

            // Шаг 5: Проверяем финальное состояние
            Log.d(TAG, "Шаг 5: Проверка финального состояния");
            int finalState = getWindowState();
            Log.d(TAG, "Финальное состояние: " + finalState);

            // Шаг 6: Возвращаем в исходное состояние
            Log.d(TAG, "Шаг 6: Возврат в исходное состояние");
            if (initialState != finalState) {
                setWindowState(initialState);
                Log.d(TAG, "Окно возвращено в исходное состояние: " + initialState);
            }

            // Итоговый отчет
            Log.d(TAG, "🏁 === ОТЧЕТ ПО ТЕСТУ ОКНА ===");
            Log.d(TAG, "Параметры: system=" + WINDOW_SYSTEM + ", event=" + WINDOW_EVENT);
            Log.d(TAG, "Начальное состояние: " + initialState);
            Log.d(TAG, "Открытие: " + (openResult ? "✅ РАБОТАЕТ" : "❌ НЕ РАБОТАЕТ"));
            Log.d(TAG, "Закрытие: " + (closeResult ? "✅ РАБОТАЕТ" : "❌ НЕ РАБОТАЕТ"));
            Log.d(TAG, "Изменения состояния: " + initialState + " → " + openState + " → " + finalState);

            if (openResult || closeResult) {
                Log.d(TAG, "🎉 ТЕСТ УСПЕШЕН! Функция управления окном работает!");
            } else {
                Log.d(TAG, "⚠️ Функция может не работать или требует других параметров");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Критическая ошибка в тесте: " + Log.getStackTraceString(e));
        }

        Log.d(TAG, "=== ТЕСТ ОКНА ЗАВЕРШЕН ===");
    }

    /**
     * Быстрый тест - только попытка открыть окно
     */
    public void quickOpenTest() {
        Log.d(TAG, "🔥 БЫСТРЫЙ ТЕСТ: Попытка открыть окно");
        Log.d(TAG, "Параметры: setInt(" + WINDOW_SYSTEM + ", " + WINDOW_EVENT + ", " + WINDOW_OPEN + ")");

        openWindow();
    }
}