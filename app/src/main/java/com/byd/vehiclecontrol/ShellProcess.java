package com.byd.vehiclecontrol;

import android.app.ActivityThread;
import android.app.Application;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.Log;

/**
 * Упрощенная версия ShellProcess для нашего проекта
 * Базовый класс для процессов с системными привилегиями
 */
public abstract class ShellProcess {
    private static final String TAG = "ShellProcess";

    protected final String processName;
    protected final String[] usedServices;
    protected Application application;
    protected Handler handler;

    public ShellProcess(String processName, String[] usedServices) {
        this.processName = processName;
        this.usedServices = usedServices != null ? usedServices : new String[0];

        try {
            initProcess();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации процесса: " + Log.getStackTraceString(e));
            throw new RuntimeException(e);
        }
    }

    private void initProcess() throws Exception {
        Log.d(TAG, "Инициализация процесса: " + processName);

        // Подготавливаем Looper если его нет
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
            Log.d(TAG, "Main Looper подготовлен");
        }

        // Инициализируем системный контекст через ActivityThread
        ActivityThread activityThread = (ActivityThread) ActivityThread.class
                .getMethod("systemMain")
                .invoke(null);

        Log.d(TAG, "ActivityThread.systemMain() выполнен");

        // Получаем Application
        this.application = ActivityThread.currentApplication();
        if (this.application == null) {
            Log.w(TAG, "Application is null, создаем заглушку");
            this.application = new Application();
        }

        // Создаем Handler
        this.handler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Процесс инициализирован успешно");
    }

    /**
     * Ожидает доступности всех необходимых системных сервисов
     */
    protected void waitForSystemServices() {
        for (String serviceName : usedServices) {
            Log.d(TAG, "Ожидание сервиса: " + serviceName);

            int attempts = 0;
            while (ServiceManager.getService(serviceName) == null && attempts < 50) {
                try {
                    Thread.sleep(100);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (ServiceManager.getService(serviceName) != null) {
                Log.d(TAG, "Сервис " + serviceName + " доступен");
            } else {
                Log.w(TAG, "Сервис " + serviceName + " не найден после ожидания");
            }
        }
    }

    /**
     * Получает системный сервис по имени
     */
    protected Object getSystemService(String serviceName) {
        try {
            IBinder binder = ServiceManager.getService(serviceName);
            if (binder == null) {
                Log.w(TAG, "Сервис " + serviceName + " не найден");
                return null;
            }

            // Пытаемся найти класс интерфейса для сервиса
            String[] possibleInterfaces = {
                    "android.hardware.automotive.vehicle.V2_0.IVehicle",
                    "android.car.hardware.CarSensorManager",
                    "android.car.CarManager",
                    serviceName + ".I" + serviceName
            };

            for (String interfaceName : possibleInterfaces) {
                try {
                    Class<?> stubClass = Class.forName(interfaceName + "$Stub");
                    Object service = stubClass.getMethod("asInterface", IBinder.class)
                            .invoke(null, binder);

                    if (service != null) {
                        Log.d(TAG, "Сервис " + serviceName + " получен через " + interfaceName);
                        return service;
                    }
                } catch (Exception e) {
                    // Пробуем следующий интерфейс
                }
            }

            Log.w(TAG, "Не удалось создать интерфейс для сервиса " + serviceName);
            return binder; // Возвращаем raw binder

        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения сервиса " + serviceName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Запускает процесс
     */
    public final void run(String... args) {
        Log.d(TAG, "Запуск процесса " + processName);

        try {
            // Ожидаем системные сервисы
            waitForSystemServices();

            // Выполняем основную логику
            execute(args);

            // Если процесс не должен работать постоянно, завершаем
            if (isOneOff()) {
                exitProcess();
            } else {
                // Запускаем event loop
                Looper.loop();
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения процесса: " + Log.getStackTraceString(e));
            exitProcess();
        }
    }

    /**
     * Основная логика процесса - должна быть переопределена в наследниках
     */
    public abstract void execute(String... args);

    /**
     * Определяет, должен ли процесс завершиться после выполнения execute()
     * По умолчанию true - процесс завершается
     */
    protected boolean isOneOff() {
        return true;
    }

    /**
     * Корректно завершает процесс
     */
    public void exitProcess() {
        Log.d(TAG, "Завершение процесса " + processName);

        try {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка завершения процесса: " + e.getMessage());
        }
    }
}