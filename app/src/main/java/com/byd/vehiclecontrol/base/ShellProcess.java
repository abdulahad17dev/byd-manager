package com.byd.vehiclecontrol.base;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.os.Process;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.Handler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class ShellProcess {
    protected String processName;
    protected Application application;
    protected Handler handler;
    private boolean isMainProcess = false;

    public ShellProcess(String processName) {
        this.processName = processName;
        System.out.println("[ShellProcess] Initializing: " + processName);

        initializeProcess();
    }

    private void initializeProcess() {
        try {
            // Проверяем, запущены ли мы как отдельный процесс или в сервисе
            if (Looper.myLooper() == null && Looper.getMainLooper() == null) {
                // Мы в отдельном процессе - полная инициализация
                System.out.println("[ShellProcess] Running as standalone process");
                isMainProcess = true;

                // Создаем системный ActivityThread
                Method systemMain = ActivityThread.class.getDeclaredMethod("systemMain");
                systemMain.invoke(null);

                // Подготавливаем главный Looper
                Looper.prepareMainLooper();
            } else {
                // Мы в сервисе - упрощенная инициализация
                System.out.println("[ShellProcess] Running in service context");
                isMainProcess = false;

                // Подготавливаем Looper для текущего потока
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
            }

            // Получаем текущий ActivityThread
            Method currentActivityThread = ActivityThread.class.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThread.invoke(null);

            if (activityThread != null) {
                createAndSetApplication(activityThread);
            } else {
                // Используем контекст из текущего приложения
                Method getCurrentApplication = ActivityThread.class.getDeclaredMethod("currentApplication");
                application = (Application) getCurrentApplication.invoke(null);
            }

            // Создаем Handler для текущего потока
            handler = new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());

            System.out.println("[ShellProcess] Process initialized successfully");

        } catch (Exception e) {
            System.err.println("[ShellProcess] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize shell process", e);
        }
    }

    private void createAndSetApplication(Object activityThread) throws Exception {
        // Получаем текущий Application для контекста
        Method getCurrentApplication = ActivityThread.class.getDeclaredMethod("currentApplication");
        Application currentApp = (Application) getCurrentApplication.invoke(null);

        // Создаем кастомный Application с обходом разрешений
        Application fakeApp = new Application() {
            @Override
            public int checkPermission(String permission, int pid, int uid) {
                return 0; // PERMISSION_GRANTED
            }

            @Override
            public int checkCallingOrSelfPermission(String permission) {
                return 0;
            }

            @Override
            public int checkSelfPermission(String permission) {
                return 0;
            }

            @Override
            public int checkCallingPermission(String permission) {
                return 0;
            }

            @Override
            public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
                return 0;
            }

            @Override
            public int checkCallingUriPermission(Uri uri, int modeFlags) {
                return 0;
            }

            @Override
            public int checkUriPermission(Uri uri, String readPermission, String writePermission,
                                          int pid, int uid, int modeFlags) {
                return 0;
            }

            @Override
            public void enforcePermission(String permission, int pid, int uid, String message) {
                // Ничего не делаем - разрешение всегда есть
            }

            @Override
            public void enforceCallingPermission(String permission, String message) {
                // Ничего не делаем
            }

            @Override
            public void enforceCallingOrSelfPermission(String permission, String message) {
                // Ничего не делаем
            }

            @Override
            public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
                // Ничего не делаем
            }

            @Override
            public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
                // Ничего не делаем
            }

            @Override
            public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
                // Ничего не делаем
            }

            @Override
            public void enforceUriPermission(Uri uri, String readPermission, String writePermission,
                                             int pid, int uid, int modeFlags, String message) {
                // Ничего не делаем
            }
        };

        if (currentApp != null) {
            // Подключаем базовый контекст
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            attach.invoke(fakeApp, currentApp.getBaseContext());

            // КРИТИЧНО: Копируем mLoadedApk из текущего Application
            // Это позволяет нашему Application использовать все системные сервисы
            Field mLoadedApkField = Application.class.getDeclaredField("mLoadedApk");
            mLoadedApkField.setAccessible(true);
            Object loadedApk = mLoadedApkField.get(currentApp);
            if (loadedApk != null) {
                mLoadedApkField.set(fakeApp, loadedApk);
                System.out.println("[ShellProcess] mLoadedApk copied successfully");
            }
        }

        // Устанавливаем наш Application в ActivityThread
        Field mInitialApplication = ActivityThread.class.getDeclaredField("mInitialApplication");
        mInitialApplication.setAccessible(true);
        mInitialApplication.set(activityThread, fakeApp);

        this.application = fakeApp;
        System.out.println("[ShellProcess] Custom Application set successfully");
    }

    // Получение системного сервиса
    public IBinder getSystemService(String serviceName) {
        try {
            return ServiceManager.getService(serviceName);
        } catch (Exception e) {
            System.err.println("[ShellProcess] Failed to get service: " + serviceName);
            e.printStackTrace();
            return null;
        }
    }

    // Абстрактный метод для реализации в наследниках
    public abstract void execute(String[] args);

    // Запуск процесса
    public void run(String[] args) {
        try {
            System.out.println("[ShellProcess] Starting execution...");

            // Выполняем основную логику
            execute(args);

            // Запускаем цикл обработки сообщений только если есть Looper
            if (Looper.myLooper() != null) {
                System.out.println("[ShellProcess] Starting message loop...");
                Looper.loop();
            } else {
                System.out.println("[ShellProcess] No Looper - running in direct mode");
                // Держим процесс живым
                keepAlive();
            }

        } catch (Exception e) {
            System.err.println("[ShellProcess] Execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для поддержания процесса живым без Looper
    private void keepAlive() {
        try {
            synchronized (this) {
                while (true) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("[ShellProcess] Process interrupted");
        }
    }

    // Завершение процесса
    protected void exitProcess() {
        System.out.println("[ShellProcess] Exiting process...");
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}