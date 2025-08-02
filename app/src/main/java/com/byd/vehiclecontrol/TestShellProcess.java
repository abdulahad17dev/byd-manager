package com.byd.vehiclecontrol;

import com.byd.vehiclecontrol.base.ShellProcess;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Method;

public class TestShellProcess extends ShellProcess {
    private static final String TAG = "TestShellProcess";

    public TestShellProcess() {
        super("TestShellProcess");
    }

    @Override
    public void execute(String[] args) {
        System.out.println("[TestShellProcess] Executing with args: " + args.length);
        Log.d(TAG, "Shell process started successfully!");

        // Тест: получаем activity service
        testActivityService();

        // Тест: проверяем разрешения
        testPermissions();

        // Тест: пробуем выполнить системную операцию
        testSystemOperation();

        // Периодическая задача
        if (handler != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Shell process is alive: " + System.currentTimeMillis());
                    handler.postDelayed(this, 5000); // каждые 5 секунд
                }
            }, 5000);
        }
    }

    private void testActivityService() {
        System.out.println("\n[TEST] Getting Activity Service...");
        IBinder activityBinder = getSystemService("activity");
        if (activityBinder != null) {
            System.out.println("[TEST] ✓ Activity service obtained!");
            System.out.println("[TEST] Binder: " + activityBinder.getClass().getName());
        } else {
            System.out.println("[TEST] ✗ Failed to get activity service");
        }
    }

    private void testPermissions() {
        System.out.println("\n[TEST] Checking permissions...");
        String[] permissions = {
                "android.permission.READ_PHONE_STATE",
                "android.permission.WRITE_SECURE_SETTINGS",
                "android.permission.INSTALL_PACKAGES"
        };

        for (String permission : permissions) {
            int result = application.checkSelfPermission(permission);
            System.out.println("[TEST] " + permission + " = " +
                    (result == 0 ? "GRANTED" : "DENIED"));
        }
    }

    private void testSystemOperation() {
        System.out.println("\n[TEST] Testing system operations...");

        try {
            // Тест: получаем список запущенных процессов
            IBinder activityBinder = getSystemService("activity");
            if (activityBinder != null) {
                // Используем рефлексию для вызова getRunningAppProcesses
                Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
                Class<?> stubClass = Class.forName("android.app.IActivityManager$Stub");

                Method asInterface = stubClass.getDeclaredMethod("asInterface", IBinder.class);
                Object activityManager = asInterface.invoke(null, activityBinder);

                Method getRunningApps = iActivityManagerClass.getDeclaredMethod("getRunningAppProcesses");
                Object result = getRunningApps.invoke(activityManager);

                System.out.println("[TEST] ✓ Got running processes: " + result);
                Log.d(TAG, "System operation successful!");
            }
        } catch (Exception e) {
            System.out.println("[TEST] ✗ System operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}