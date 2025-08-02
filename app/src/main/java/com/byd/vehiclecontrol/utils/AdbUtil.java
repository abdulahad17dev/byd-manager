package com.byd.vehiclecontrol.utils;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class AdbUtil {
    private static final String TAG = "AdbUtil";
    private static final String ADB_HOST = "127.0.0.1";
    private static final int ADB_PORT = 5555;

    public static boolean runShellCommandOnce(Context context, String... commands) {
        try {
            // Подключаемся к локальному ADB демону
            Socket socket = new Socket(ADB_HOST, ADB_PORT);
            OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Отправляем команды
            for (String command : commands) {
                Log.d(TAG, "Executing command: " + command);
                writer.write("shell:" + command + "\n");
                writer.flush();

                // Читаем ответ
                String response = reader.readLine();
                Log.d(TAG, "Command response: " + response);
            }

            socket.close();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute ADB commands", e);

            // Fallback: пытаемся выполнить через Runtime.exec
            try {
                for (String command : commands) {
                    Log.d(TAG, "Fallback: executing via Runtime.exec: " + command);
                    Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                    process.waitFor();
                }
                return true;
            } catch (Exception fallbackException) {
                Log.e(TAG, "Fallback execution also failed", fallbackException);
                return false;
            }
        }
    }

    public static boolean isAdbConnectable(Context context) {
        try {
            Socket socket = new Socket(ADB_HOST, ADB_PORT);
            socket.close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "ADB not connectable: " + e.getMessage());
            return false;
        }
    }
}
