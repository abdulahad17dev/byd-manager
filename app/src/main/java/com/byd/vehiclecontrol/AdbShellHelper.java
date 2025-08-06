package com.byd.vehiclecontrol;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.File;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class AdbShellHelper {
    private static final String TAG = "AdbShellHelper";
    private static final String HOST = "127.0.0.1";  // IP adb over TCP
    private static final int PORT = 5555;

    private static AdbCrypto crypto;
    private static AdbConnection connection;
    private static boolean isConnected = false;
    private static Context appContext;
    private static final String APP_PROCESS_CLASS = "com.byd.vehiclecontrol.VoiceAssistantProcess";
    private static Process appProcess = null;
    private static boolean isAppProcessRunning = false;

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }

    private static ConnectionCallback connectionCallback;

    public static void setConnectionCallback(ConnectionCallback callback) {
        connectionCallback = callback;
    }

    public static boolean isConnected() {
        return isConnected && connection != null;
    }

    // Автоматическая проверка и подключение при запуске
    public static void autoConnectOnStartup(Context context) {
        appContext = context.getApplicationContext();

        new Thread(() -> {
            try {
                // Проверяем доступность ADB через TCP
                if (isAdbTcpEnabled()) {
                    // Если TCP доступен, пытаемся подключиться автоматически
                    Log.d(TAG, "ADB TCP доступен, пытаемся автоподключение...");
                    connectInternal(false); // false = не показывать Toast об ошибках TCP
                } else {
                    Log.d(TAG, "ADB TCP недоступен при запуске");
                    if (connectionCallback != null) {
                        connectionCallback.onError("ADB TCP не включен");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при автоподключении: " + e.getMessage());
            }
        }).start();
    }

    public static void connect(Context context) {
        appContext = context.getApplicationContext();
        connectInternal(true); // true = показывать Toast об ошибках TCP
    }

    private static void connectInternal(boolean showTcpErrorToast) {
        new Thread(() -> {
            try {
                // Проверяем доступность ADB через TCP
                if (!isAdbTcpEnabled()) {
                    if (showTcpErrorToast) {
                        runOnUiThread(() -> {
                            Toast.makeText(appContext,
                                    "Включите отладку по Wi-Fi в настройках разработчика:\n" +
                                            "Настройки → Система → Параметры разработчика → Отладка по Wi-Fi",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                    if (connectionCallback != null) {
                        connectionCallback.onError("ADB TCP не включен");
                    }
                    return;
                }

                AdbBase64 base64 = data -> Base64.encodeToString(data, Base64.NO_WRAP);
                File privKey = new File(appContext.getFilesDir(), "adbkey");
                File pubKey = new File(appContext.getFilesDir(), "adbkey.pub");

                if (privKey.exists() && pubKey.exists()) {
                    crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
                    Log.d(TAG, "ADB ключи загружены");
                } else {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    crypto.saveAdbKeyPair(privKey, pubKey);
                    Log.d(TAG, "ADB ключи созданы. Нужно авторизовать устройство");

                    if (showTcpErrorToast) { // Показываем Toast только при ручном подключении
                        runOnUiThread(() -> {
                            Toast.makeText(appContext,
                                    "Первое подключение: разрешите отладку в диалоге на экране",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }

                Socket socket = new Socket(HOST, PORT);
                connection = AdbConnection.create(socket, crypto);

                // Подключаемся с таймаутом
                boolean connected = connection.connect(10, TimeUnit.SECONDS, false);

                if (connected) {
                    isConnected = true;
                    Log.d(TAG, "Соединение с ADB установлено");

                    if (showTcpErrorToast) { // Показываем Toast только при ручном подключении
                        runOnUiThread(() -> {
                            Toast.makeText(appContext, "ADB подключено успешно", Toast.LENGTH_SHORT).show();
                        });
                    }

                    if (connectionCallback != null) {
                        connectionCallback.onConnected();
                    }
                } else {
                    throw new Exception("Таймаут подключения к ADB");
                }

            } catch (ConnectException e) {
                Log.e(TAG, "Не удается подключиться к ADB: " + e.getMessage());
                if (showTcpErrorToast) { // Показываем Toast только при ручном подключении
                    runOnUiThread(() -> {
                        Toast.makeText(appContext,
                                "Включите отладку по Wi-Fi и перезапустите ADB:\n" +
                                        "adb tcpip 5555",
                                Toast.LENGTH_LONG).show();
                    });
                }
                if (connectionCallback != null) {
                    connectionCallback.onError("Нет подключения к ADB TCP");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при подключении: " + Log.getStackTraceString(e));
                if (showTcpErrorToast) { // Показываем Toast только при ручном подключении
                    runOnUiThread(() -> {
                        Toast.makeText(appContext, "Ошибка подключения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
                if (connectionCallback != null) {
                    connectionCallback.onError(e.getMessage());
                }
                isConnected = false;
            }
        }).start();
    }

    public static void disconnect() {
        new Thread(() -> {
            try {
                if (connection != null) {
                    connection.close();
                    connection = null;
                }
                isConnected = false;
                Log.d(TAG, "ADB соединение закрыто");

                if (connectionCallback != null) {
                    connectionCallback.onDisconnected();
                }

                runOnUiThread(() -> {
                    Toast.makeText(appContext, "ADB отключено", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при отключении: " + Log.getStackTraceString(e));
            }
        }).start();
    }

    // Проверяем доступность ADB TCP порта
    private static boolean isAdbTcpEnabled() {
        try {
            Socket testSocket = new Socket();
            testSocket.connect(new java.net.InetSocketAddress(HOST, PORT), 3000);
            testSocket.close();
            return true;
        } catch (Exception e) {
            Log.d(TAG, "ADB TCP порт недоступен: " + e.getMessage());
            return false;
        }
    }

    // Одноразовая команда с проверкой подключения
    public static void runSingleCommand(String command) {
        if (!isConnected()) {
            runOnUiThread(() -> {
                Toast.makeText(appContext, "Сначала подключитесь к ADB", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new Thread(() -> {
            try {
                AdbStream stream = connection.open("shell:" + command);
                StringBuilder result = new StringBuilder();

                // Читаем ответ с таймаутом
                long startTime = System.currentTimeMillis();
                while (!stream.isClosed() && (System.currentTimeMillis() - startTime) < 5000) {
                    try {
                        byte[] data = stream.read();
                        if (data != null) {
                            result.append(new String(data));
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                Log.d(TAG, "Команда: " + command);
                Log.d(TAG, "Результат: " + result.toString());

                final String resultStr = result.toString();
                runOnUiThread(() -> {
                    if (resultStr.trim().isEmpty()) {
                        Toast.makeText(appContext, "Команда выполнена: " + command, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(appContext, "Результат: " + resultStr.trim(), Toast.LENGTH_SHORT).show();
                    }
                });

                stream.close();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при выполнении команды: " + Log.getStackTraceString(e));
                runOnUiThread(() -> {
                    Toast.makeText(appContext, "Ошибка выполнения команды: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

                // Если соединение потеряно, обновляем статус
                if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                    isConnected = false;
                    if (connectionCallback != null) {
                        connectionCallback.onDisconnected();
                    }
                }
            }
        }).start();
    }

    // Интерактивная сессия с проверкой подключения
    public static void runInteractiveSession(String[] commands) {
        if (!isConnected()) {
            runOnUiThread(() -> {
                Toast.makeText(appContext, "Сначала подключитесь к ADB", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new Thread(() -> {
            try {
                AdbStream stream = connection.open("shell:");

                for (String cmd : commands) {
                    stream.write(cmd + "\n");
                    Log.d(TAG, "Отправлено: " + cmd);

                    Thread.sleep(500); // пауза между командами

                    try {
                        byte[] response = stream.read();
                        if (response != null) {
                            Log.d(TAG, "Ответ: " + new String(response));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Не удалось прочитать ответ: " + e.getMessage());
                    }
                }

                stream.close();

                runOnUiThread(() -> {
                    Toast.makeText(appContext, "Интерактивная сессия завершена", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка в интерактивной сессии: " + Log.getStackTraceString(e));
                runOnUiThread(() -> {
                    Toast.makeText(appContext, "Ошибка сессии: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Проверка статуса подключения
    public static void checkConnectionStatus() {
        runOnUiThread(() -> {
            if (isConnected()) {
                Toast.makeText(appContext, "✅ ADB подключено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(appContext, "❌ ADB не подключено", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Утилитарный метод для выполнения кода в UI потоке
    private static void runOnUiThread(Runnable runnable) {
        if (appContext != null) {
            if (appContext instanceof android.app.Activity) {
                ((android.app.Activity) appContext).runOnUiThread(runnable);
            } else {
                // Для случаев когда у нас только контекст приложения
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(runnable);
            }
        }
    }

    public static void startAppProcess(Context context) {
        if (!isConnected()) {
            runOnUiThread(() -> {
                Toast.makeText(context, "Сначала подключитесь к ADB", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new Thread(() -> {
            try {
                // Проверяем, что соединение все еще активно
                if (connection == null) {
                    Log.e(TAG, "Соединение с ADB потеряно, переподключаемся...");
                    
                    // Пытаемся переподключиться
                    connect(context);
                    Thread.sleep(1000); // Даем время на подключение
                    
                    if (!isConnected()) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Не удалось подключиться к ADB", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }
                
                // Получаем путь к APK файлу
                String apkPath = context.getApplicationInfo().sourceDir;
                Log.d(TAG, "APK Path: " + apkPath);

                // Получаем cache directory для рабочей директории
                String cacheDir = "/data/data/" + context.getPackageName() + "/cache";

                // Формируем команду app_process с правильными параметрами
                // Используем CLASSPATH для надежной загрузки классов
                String command = String.format(
                        "CLASSPATH=%s app_process /system/bin %s &",
                        apkPath,
                        APP_PROCESS_CLASS
                );

                Log.d(TAG, "Запуск app_process: " + command);

                // Запускаем app_process через ADB shell
                AdbStream stream = connection.open("shell:" + command);

                // Читаем начальный вывод
                StringBuilder output = new StringBuilder();
                long startTime = System.currentTimeMillis();

                while ((System.currentTimeMillis() - startTime) < 3000) {
                    try {
                        byte[] data = stream.read();
                        if (data != null) {
                            String response = new String(data);
                            output.append(response);
                            Log.d(TAG, "app_process output: " + response);
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                // Проверяем, запустился ли процесс
                Thread.sleep(1000);
                checkAppProcessStatus();

                runOnUiThread(() -> {
                    Toast.makeText(context, "app_process запущен", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка запуска app_process: " + Log.getStackTraceString(e));
                runOnUiThread(() -> {
                    Toast.makeText(context, "Ошибка запуска: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Проверка статуса app_process
    public static void checkAppProcessStatus() {
        if (!isConnected()) {
            return;
        }

        new Thread(() -> {
            try {
                // Проверяем активность соединения
                if (connection == null) {
                    Log.w(TAG, "Соединение неактивно при проверке статуса процесса");
                    isAppProcessRunning = false;
                    return;
                }
                
                // Ищем наш процесс в списке запущенных процессов
                // app_process запускается с CLASSPATH нашего APK, ищем по пути APK
                AdbStream stream = connection.open("shell:ps -A | grep app_process");

                StringBuilder result = new StringBuilder();
                long startTime = System.currentTimeMillis();

                while ((System.currentTimeMillis() - startTime) < 2000) {
                    try {
                        byte[] data = stream.read();
                        if (data != null) {
                            result.append(new String(data));
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                stream.close();

                String processInfo = result.toString().trim();
                isAppProcessRunning = !processInfo.isEmpty();

                Log.d(TAG, "Process check result: " + processInfo);
                Log.d(TAG, "App process running: " + isAppProcessRunning);

                runOnUiThread(() -> {
                    String message = isAppProcessRunning ?
                            "✅ app_process запущен: " + processInfo :
                            "❌ app_process не найден";
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка проверки app_process: " + Log.getStackTraceString(e));
            }
        }).start();
    }

    // Остановка app_process
    public static void stopAppProcess() {
        if (!isConnected()) {
            return;
        }

        new Thread(() -> {
            try {
                // Убиваем процесс по имени класса
                AdbStream stream = connection.open("shell:kill -9 $(ps -A | grep " + APP_PROCESS_CLASS + " | awk 'NR==1{print $2}')");

                Thread.sleep(500);
                stream.close();

                isAppProcessRunning = false;

                runOnUiThread(() -> {
                    Toast.makeText(appContext, "app_process остановлен", Toast.LENGTH_SHORT).show();
                });

                Log.d(TAG, "app_process остановлен");

            } catch (Exception e) {
                Log.e(TAG, "Ошибка остановки app_process: " + Log.getStackTraceString(e));
            }
        }).start();
    }

    // Проверка запущен ли app_process
    public static boolean isAppProcessRunning() {
        return isAppProcessRunning;
    }
}