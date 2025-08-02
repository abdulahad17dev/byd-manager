package com.byd.vehiclecontrol.shell;

import android.util.Log;

/**
 * Главный класс для запуска через app_process
 * Этот класс будет запущен в привилегированном процессе через ADB
 */
public class CommunicationProcessMain {
    private static final String TAG = "CommunicationProcessMain";

    public static void main(String[] args) {
        Log.d(TAG, "CommunicationProcessMain started with args: " + java.util.Arrays.toString(args));

        try {
            // Запускаем CommunicationProcess
            CommunicationProcess.INSTANCE.run(args);

            // Держим процесс живым
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            Log.e(TAG, "CommunicationProcessMain failed", e);
            System.exit(1);
        }
    }
}
