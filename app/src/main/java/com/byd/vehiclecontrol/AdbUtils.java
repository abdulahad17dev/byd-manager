package com.byd.vehiclecontrol;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Утилиты для работы с ADB клиентом
 */
public class AdbUtils {

    // Константы
    public static final String DEFAULT_HOST = "172.20.10.2";
    public static final int DEFAULT_PORT = 5555;
    public static final long DEFAULT_TIMEOUT_MS = 10000;

    /**
     * Простая проверка доступности ADB демона
     */
    public static boolean isAdbAvailable() {
        return isAdbAvailable(DEFAULT_HOST, DEFAULT_PORT);
    }

    public static boolean isAdbAvailable(String host, int port) {
        return AdbClient.isAdbAvailable(host, port); // ИСПРАВЛЕНО: вызов метода из AdbClient
    }

    /**
     * Создание ADB соединения с дефолтными параметрами
     */
    public static AdbClient connect() throws IOException, InterruptedException {
        return connect(DEFAULT_HOST, DEFAULT_PORT);
    }

    public static AdbClient connect(String host, int port) throws IOException, InterruptedException {
        return AdbClient.connectShell(host, port);
    }

    /**
     * Асинхронное выполнение команды с таймаутом
     */
    public static CompletableFuture<String> executeCommandAsync(String command) {
        return executeCommandAsync(DEFAULT_HOST, DEFAULT_PORT, command, DEFAULT_TIMEOUT_MS);
    }

    public static CompletableFuture<String> executeCommandAsync(String host, int port, String command, long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            try (AdbClient client = AdbClient.connectShell(host, port)) {
                return client.executeCommand(command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Выполнение одной команды (создает соединение, выполняет команду, закрывает)
     */
    public static String executeCommandOnce(String command) throws IOException, InterruptedException {
        return executeCommandOnce(DEFAULT_HOST, DEFAULT_PORT, command);
    }

    public static String executeCommandOnce(String host, int port, String command) throws IOException, InterruptedException {
        try (AdbClient client = AdbClient.connectShell(host, port)) {
            return client.executeCommand(command);
        }
    }

    /**
     * Выполнение нескольких команд в одном сеансе
     */
    public static String[] executeCommands(String... commands) throws IOException, InterruptedException {
        return executeCommands(DEFAULT_HOST, DEFAULT_PORT, commands);
    }

    public static String[] executeCommands(String host, int port, String... commands) throws IOException, InterruptedException {
        try (AdbClient client = AdbClient.connectShell(host, port)) {
            String[] results = new String[commands.length];
            for (int i = 0; i < commands.length; i++) {
                results[i] = client.executeCommand(commands[i]);
            }
            return results;
        }
    }

    /**
     * Популярные Android команды
     */
    public static class AndroidCommands {

        // Системная информация
        public static final String GET_ANDROID_VERSION = "getprop ro.build.version.release";
        public static final String GET_API_LEVEL = "getprop ro.build.version.sdk";
        public static final String GET_DEVICE_MODEL = "getprop ro.product.model";
        public static final String GET_DEVICE_BRAND = "getprop ro.product.brand";
        public static final String GET_SERIAL_NUMBER = "getprop ro.serialno";

        // Пакеты и активности
        public static final String LIST_PACKAGES = "pm list packages";
        public static final String LIST_SYSTEM_PACKAGES = "pm list packages -s";
        public static final String LIST_THIRD_PARTY_PACKAGES = "pm list packages -3";
        public static final String GET_CURRENT_ACTIVITY = "dumpsys activity | grep mCurrentFocus";

        // Настройки
        public static final String ENABLE_USB_DEBUGGING = "setprop service.adb.tcp.port 5555";
        public static final String DISABLE_USB_DEBUGGING = "setprop service.adb.tcp.port -1";

        // Файловая система
        public static final String LIST_ROOT = "ls /";
        public static final String GET_STORAGE_INFO = "df -h";
        public static final String GET_MEMORY_INFO = "cat /proc/meminfo";

        // Процессы
        public static final String LIST_PROCESSES = "ps";
        public static final String GET_TOP_PROCESSES = "top -n 1";

        // Сеть
        public static final String GET_IP_ADDRESS = "ip addr show wlan0";
        public static final String GET_WIFI_INFO = "dumpsys wifi";

        /**
         * Утилиты для работы с командами
         */
        public static String installApk(String apkPath) {
            return "pm install " + apkPath;
        }

        public static String uninstallPackage(String packageName) {
            return "pm uninstall " + packageName;
        }

        public static String startActivity(String packageName, String activityName) {
            return String.format("am start -n %s/%s", packageName, activityName);
        }

        public static String stopApp(String packageName) {
            return "am force-stop " + packageName;
        }

        public static String takeScreenshot(String outputPath) {
            return "screencap -p " + outputPath;
        }

        public static String pullFile(String devicePath, String localPath) {
            return String.format("cp %s %s", devicePath, localPath);
        }

        public static String pushFile(String localPath, String devicePath) {
            return String.format("cp %s %s", localPath, devicePath);
        }

        public static String getPackageInfo(String packageName) {
            return "dumpsys package " + packageName;
        }

        public static String enablePackage(String packageName) {
            return "pm enable " + packageName;
        }

        public static String disablePackage(String packageName) {
            return "pm disable " + packageName;
        }

        public static String clearAppData(String packageName) {
            return "pm clear " + packageName;
        }

        public static String grantPermission(String packageName, String permission) {
            return String.format("pm grant %s %s", packageName, permission);
        }

        public static String revokePermission(String packageName, String permission) {
            return String.format("pm revoke %s %s", packageName, permission);
        }

        public static String setProperty(String key, String value) {
            return String.format("setprop %s %s", key, value);
        }

        public static String getProperty(String key) {
            return "getprop " + key;
        }

        public static String reboot() {
            return "reboot";
        }

        public static String rebootRecovery() {
            return "reboot recovery";
        }

        public static String rebootBootloader() {
            return "reboot bootloader";
        }
    }

    /**
     * Вспомогательные методы для частых операций
     */
    public static class DeviceInfo {

        public static String getAndroidVersion() throws IOException, InterruptedException {
            return executeCommandOnce(AndroidCommands.GET_ANDROID_VERSION).trim();
        }

        public static String getApiLevel() throws IOException, InterruptedException {
            return executeCommandOnce(AndroidCommands.GET_API_LEVEL).trim();
        }

        public static String getDeviceModel() throws IOException, InterruptedException {
            return executeCommandOnce(AndroidCommands.GET_DEVICE_MODEL).trim();
        }

        public static String getDeviceBrand() throws IOException, InterruptedException {
            return executeCommandOnce(AndroidCommands.GET_DEVICE_BRAND).trim();
        }

        public static String getSerialNumber() throws IOException, InterruptedException {
            return executeCommandOnce(AndroidCommands.GET_SERIAL_NUMBER).trim();
        }

        public static String getCurrentActivity() throws IOException, InterruptedException {
            String result = executeCommandOnce(AndroidCommands.GET_CURRENT_ACTIVITY);
            // Парсим результат для извлечения имени активности
            if (result.contains("mCurrentFocus")) {
                int start = result.indexOf("Window{");
                int end = result.indexOf("}", start);
                if (start != -1 && end != -1) {
                    String window = result.substring(start, end + 1);
                    // Извлекаем имя активности из строки Window
                    String[] parts = window.split(" ");
                    for (String part : parts) {
                        if (part.contains("/")) {
                            return part;
                        }
                    }
                }
            }
            return result.trim();
        }

        public static boolean isPackageInstalled(String packageName) throws IOException, InterruptedException {
            String result = executeCommandOnce("pm list packages " + packageName);
            return result.contains("package:" + packageName);
        }

        public static String[] getInstalledPackages() throws IOException, InterruptedException {
            String result = executeCommandOnce(AndroidCommands.LIST_PACKAGES);
            String[] lines = result.split("\n");
            String[] packages = new String[lines.length];

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("package:")) {
                    packages[i] = line.substring(8); // Убираем "package:"
                } else {
                    packages[i] = line;
                }
            }

            return packages;
        }
    }

    /**
     * Класс для работы с результатами команд
     */
    public static class CommandResult {
        private final String output;
        private final boolean success;
        private final Exception error;

        public CommandResult(String output, boolean success, Exception error) {
            this.output = output;
            this.success = success;
            this.error = error;
        }

        public static CommandResult success(String output) {
            return new CommandResult(output, true, null);
        }

        public static CommandResult failure(Exception error) {
            return new CommandResult(null, false, error);
        }

        public String getOutput() { return output; }
        public boolean isSuccess() { return success; }
        public Exception getError() { return error; }

        @Override
        public String toString() {
            return success ? output : ("Error: " + error.getMessage());
        }
    }

    /**
     * Безопасное выполнение команды с обработкой ошибок
     */
    public static CommandResult safeExecuteCommand(String command) {
        return safeExecuteCommand(DEFAULT_HOST, DEFAULT_PORT, command);
    }

    public static CommandResult safeExecuteCommand(String host, int port, String command) {
        try {
            String result = executeCommandOnce(host, port, command);
            return CommandResult.success(result);
        } catch (Exception e) {
            return CommandResult.failure(e);
        }
    }
}