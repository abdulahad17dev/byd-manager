package com.byd.vehiclecontrol;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.byd.vehiclecontrol.shell.CommunicationBinder;
import com.byd.vehiclecontrol.shell.CommunicationProcess;

/**
 * Главный класс процесса, который запускается через app_process
 * с системными привилегиями. Эквивалент CommunicationProcess из оригинала.
 */
public class VoiceAssistantProcess {
    private static final String TAG = "VoiceAssistantProcess";
    private static final String ACTION_COMMUNICATION_PROCESS_STARTED = "ACTION_voice_assistant_process_started";
    private CommunicationBinder communicationBinder;
    private CommunicationProcess communicationProcess;

    /**
     * Точка входа для app_process
     * Эквивалент main функции из CommunicationProcessKt
     */
    public static void main(String[] args) {
        // Используем System.out для отладки, так как Log может не работать
        System.out.println("=== VoiceAssistantProcess started ===");
        System.out.println("Process ID: " + android.os.Process.myPid());
        System.out.println("User ID: " + android.os.Process.myUid());
        System.out.println("Arguments: " + java.util.Arrays.toString(args));

        try {
            // Создаем экземпляр процесса и запускаем его
            VoiceAssistantProcess process = new VoiceAssistantProcess();
            process.run(args);

        } catch (Throwable e) {
            System.err.println("Критическая ошибка в VoiceAssistantProcess: " + e.getMessage());
            e.printStackTrace();

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
            System.out.println("Инициализация VoiceAssistantProcess...");

            // Сначала попробуем самый простой тест
            System.out.println("Шаг 1: Базовая проверка...");
            Thread.sleep(100);
            
            System.out.println("Шаг 2: Инициализация Android runtime...");
            // Подготавливаем Android runtime среду
            initAndroidRuntime();
            
            System.out.println("Шаг 3: Создание Binder...");
            // Создаем и регистрируем Binder для IPC
            setupBinder();

            System.out.println("Шаг 4: Выполнение основной логики...");
            // Выполняем основную логику
            execute(args);

            System.out.println("Шаг 5: Отправка broadcast...");
            // Отправляем broadcast что процесс запущен
            sendBroadcast();

            System.out.println("Шаг 6: Тестирование системных сервисов...");
            // Тестируем доступ к системным сервисам
            testSystemServices();

            System.out.println("✅ Инициализация завершена. Процесс готов к работе!");

            // Запускаем event loop для постоянной работы
            System.out.println("Запуск event loop...");
            Looper.loop();

        } catch (Throwable e) {
            System.err.println("Ошибка в run(): " + e.getMessage());
            e.printStackTrace();
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
     * Создает и регистрирует CommunicationBinder для межпроцессного взаимодействия
     */
    private void setupBinder() {
        try {
            System.out.println("setupBinder: Начинаем создание CommunicationBinder...");
            
            Application app = ActivityThread.currentApplication();
            System.out.println("setupBinder: Application = " + app);
            
            if (app != null) {
                System.out.println("setupBinder: Создаем CommunicationProcess...");
                // Создаем наследника CommunicationProcess с защищенным конструктором
                communicationProcess = new CommunicationProcess("VoiceAssistantProcess") {
                    @Override
                    public Application getApplication() {
                        return ActivityThread.currentApplication();
                    }
                    
                    @Override
                    public void execute(String[] args) {
                        // Не используется в этом контексте
                    }
                };
                
                System.out.println("setupBinder: CommunicationProcess создан");
                
                // Устанавливаем application field через рефлексию
                try {
                    java.lang.reflect.Field appField = CommunicationProcess.class
                            .getSuperclass().getDeclaredField("application");
                    appField.setAccessible(true);
                    appField.set(communicationProcess, app);
                    System.out.println("setupBinder: Application field установлен");
                } catch (Exception e) {
                    System.err.println("setupBinder: Не удалось установить application field: " + e.getMessage());
                }
                
                // Создаем Binder
                System.out.println("setupBinder: Создаем CommunicationBinder...");
                communicationBinder = new CommunicationBinder(communicationProcess);
                System.out.println("setupBinder: CommunicationBinder создан = " + communicationBinder);
                
                // Регистрируем Binder глобально через VehicleApplication
                System.out.println("setupBinder: Регистрируем Binder глобально...");
                registerBinderGlobally(communicationBinder);
                
                System.out.println("✅ setupBinder: Binder успешно зарегистрирован для IPC");
            } else {
                System.err.println("❌ setupBinder: Application is null, не можем создать Binder");
            }
            
        } catch (Throwable e) {
            System.err.println("setupBinder: КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Регистрирует Binder глобально для доступа из основного процесса
     */
    private void registerBinderGlobally(IBinder binder) {
        try {
            // Сохраняем в глобальный статический экземпляр
            CommunicationBinder.setGlobalInstance((CommunicationBinder) binder);
            System.out.println("✅ Binder сохранен в CommunicationBinder.globalInstance");
            
            // Пытаемся установить в VehicleBinderService напрямую
            Class<?> serviceClass = Class.forName("com.byd.vehiclecontrol.VehicleBinderService");
            java.lang.reflect.Field shellBinderField = serviceClass.getDeclaredField("sShellBinder");
            shellBinderField.setAccessible(true);
            shellBinderField.set(null, binder);
            System.out.println("✅ Binder установлен в VehicleBinderService.sShellBinder");
            
            // Дополнительно пытаемся зарегистрировать как системный сервис (может не работать без прав)
            try {
                Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
                java.lang.reflect.Method addServiceMethod = serviceManagerClass.getDeclaredMethod(
                        "addService", String.class, IBinder.class);
                addServiceMethod.setAccessible(true);
                addServiceMethod.invoke(null, "byd.vehicle.control", binder);
                System.out.println("✅ Binder также зарегистрирован как системный сервис");
            } catch (Exception sysErr) {
                System.out.println("⚠️ Не удалось зарегистрировать как системный сервис (требуются права): " + sysErr.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Ошибка регистрации Binder: " + e.getMessage());
            e.printStackTrace();
        }
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