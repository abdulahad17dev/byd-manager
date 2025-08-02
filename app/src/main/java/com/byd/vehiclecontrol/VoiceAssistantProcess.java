package com.byd.vehiclecontrol;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Главный класс процесса, который запускается через app_process
 * с системными привилегиями. Эквивалент CommunicationProcess из оригинала.
 */
public class VoiceAssistantProcess {
    private static final String TAG = "VoiceAssistantProcess";
    private static final String ACTION_COMMUNICATION_PROCESS_STARTED = "ACTION_voice_assistant_process_started";

    /**
     * Точка входа для app_process
     * Эквивалент main функции из CommunicationProcessKt
     */
    public static void main(String[] args) {
        Log.d(TAG, "=== VoiceAssistantProcess started ===");
        Log.d(TAG, "Process ID: " + android.os.Process.myPid());
        Log.d(TAG, "User ID: " + android.os.Process.myUid());
        Log.d(TAG, "Arguments: " + java.util.Arrays.toString(args));

        try {
            // Создаем экземпляр процесса и запускаем его
            VoiceAssistantProcess process = new VoiceAssistantProcess();
            process.run(args);

        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка в VoiceAssistantProcess: " +
                    android.util.Log.getStackTraceString(e));

            // Принудительно завершаем процесс при ошибке
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * Основная логика процесса
     */
    public void run(String[] args) {
        try {
            Log.d(TAG, "Инициализация VoiceAssistantProcess...");

            // Подготавливаем Android runtime среду
            initAndroidRuntime();

            // Выполняем основную логику
            execute(args);

            // Отправляем broadcast что процесс запущен
            sendBroadcast();

            // Тестируем доступ к системным сервисам
            testSystemServices();

            Log.d(TAG, "✅ Инициализация завершена. Процесс готов к работе!");

            // Запускаем event loop для постоянной работы
            Log.d(TAG, "Запуск event loop...");
            Looper.loop();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в run(): " + android.util.Log.getStackTraceString(e));
            exitProcess();
        }
    }

    /**
     * Инициализирует Android runtime среду
     */
    private void initAndroidRuntime() throws Exception {
        Log.d(TAG, "Инициализация Android runtime...");

        // Подготавливаем Looper если его нет
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
            Log.d(TAG, "Main Looper подготовлен");
        }

        // Инициализируем ActivityThread для системного контекста
        Object activityThread = ActivityThread.class
                .getMethod("systemMain")
                .invoke(null);

        Log.d(TAG, "ActivityThread.systemMain() выполнен: " + activityThread);

        // Получаем Application
        Application application = ActivityThread.currentApplication();
        if (application == null) {
            Log.w(TAG, "Application is null после systemMain()");
        } else {
            Log.d(TAG, "Application получен: " + application.getClass().getName());
        }

        Log.d(TAG, "Android runtime инициализирован успешно");
    }

    /**
     * Основная логика - можно переопределить в наследниках
     */
    protected void execute(String[] args) {
        Log.d(TAG, "Выполнение основной логики процесса...");
        // Основная логика выполняется в run()
    }

    /**
     * Отправляет broadcast уведомление что процесс запущен
     */
    private void sendBroadcast() {
        try {
            Log.d(TAG, "Отправка broadcast уведомления...");

            Application application = ActivityThread.currentApplication();
            if (application == null) {
                Log.w(TAG, "Application is null, не можем отправить broadcast");
                return;
            }

            Intent intent = new Intent(ACTION_COMMUNICATION_PROCESS_STARTED);
            intent.setPackage("com.byd.vehiclecontrol"); // Наш package name

            Log.d(TAG, "Отправляем broadcast: " + ACTION_COMMUNICATION_PROCESS_STARTED);
            application.sendBroadcast(intent);

            // Планируем проверку через 3 секунды (как в оригинале)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "Broadcast timeout check - процесс продолжает работу");
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отправке broadcast: " +
                    android.util.Log.getStackTraceString(e));
        }
    }

    /**
     * Тестирует доступ к системным сервисам
     */
    private void testSystemServices() {
        try {
            Log.d(TAG, "Тестирование доступа к системным сервисам...");

            // Проверяем доступ к автомобильным сервисам
            testAutoService();

            // Проверяем доступ к другим системным сервисам
            testBasicSystemServices();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при тестировании сервисов: " +
                    android.util.Log.getStackTraceString(e));
        }
    }

    /**
     * Тестирует доступ к автомобильным сервисам
     */
    private void testAutoService() {
        try {
            Application application = ActivityThread.currentApplication();
            if (application == null) {
                Log.w(TAG, "Application is null, не можем тестировать auto service");
                return;
            }

            // Получаем автомобильный сервис (мы знаем что он есть из предыдущих логов)
            Object autoService = application.getSystemService("auto");

            if (autoService != null) {
                Log.d(TAG, "✅ Автомобильный сервис получен: " + autoService.getClass().getName());

                // Создаем API для управления автомобилем
                VehicleControlAPI vehicleAPI = new VehicleControlAPI(autoService);

                // Запускаем тестирование функций
                testVehicleFunctions(vehicleAPI);

            } else {
                Log.w(TAG, "❌ Автомобильный сервис недоступен");
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в testAutoService(): " +
                    android.util.Log.getStackTraceString(e));
        }
    }

    /**
     * Тестирует функции управления автомобилем
     */
    private void testVehicleFunctions(VehicleControlAPI vehicleAPI) {
        Log.d(TAG, "🚗 Начинаем тестирование функций автомобиля...");

        try {
            // СПЕЦИАЛЬНЫЙ ТЕСТ: Конкретные параметры окна
            Log.d(TAG, "=== СПЕЦИАЛЬНЫЙ ТЕСТ: Окно system:1001, event:1125122104 ===");

            WindowTestAPI windowTest = new WindowTestAPI(vehicleAPI.autoManager);

            // Запускаем полный тест окна
            windowTest.runFullWindowTest();

            Thread.sleep(2000);

            // Дополнительно: быстрый тест открытия
            Log.d(TAG, "=== ДОПОЛНИТЕЛЬНЫЙ БЫСТРЫЙ ТЕСТ ===");
            windowTest.quickOpenTest();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при тестировании функций: " +
                    android.util.Log.getStackTraceString(e));
        }

        Log.d(TAG, "🏁 Тестирование функций завершено");
    }

    /**
     * Тестирует базовые системные сервисы
     */
    private void testBasicSystemServices() {
        try {
            Application application = ActivityThread.currentApplication();
            if (application == null) return;

            // Тестируем доступ к основным сервисам
            String[] basicServices = {"activity", "window", "power", "connectivity"};

            for (String serviceName : basicServices) {
                try {
                    Object service = application.getSystemService(serviceName);
                    if (service != null) {
                        Log.d(TAG, "✅ Системный сервис '" + serviceName + "': " +
                                service.getClass().getName());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "❌ Ошибка доступа к '" + serviceName + "': " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в testBasicSystemServices(): " + e.getMessage());
        }
    }

    /**
     * Выводит методы сервиса
     */
    private void listServiceMethods(Object service, String serviceName) {
        try {
            java.lang.reflect.Method[] methods = service.getClass().getMethods();
            Log.d(TAG, "Методы сервиса '" + serviceName + "' (всего " + methods.length + "):");

            int count = 0;
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName();

                // Показываем только интересные методы
                if (methodName.contains("set") || methodName.contains("get") ||
                        methodName.contains("control") || methodName.contains("open") ||
                        methodName.contains("close") || methodName.contains("enable") ||
                        methodName.contains("disable") || methodName.contains("window")) {

                    Log.d(TAG, "  - " + methodName + "(" +
                            java.util.Arrays.toString(method.getParameterTypes()) + ")");
                    count++;

                    // Ограничиваем вывод
                    if (count >= 15) {
                        Log.d(TAG, "  ... и еще " + (methods.length - count) + " методов");
                        break;
                    }
                }
            }

            if (count == 0) {
                Log.d(TAG, "  Специфичные методы не найдены");
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при выводе методов: " + e.getMessage());
        }
    }

    /**
     * Корректно завершает процесс
     */
    private void exitProcess() {
        Log.d(TAG, "Завершение VoiceAssistantProcess");

        try {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка завершения процесса: " + e.getMessage());
        }
    }
}