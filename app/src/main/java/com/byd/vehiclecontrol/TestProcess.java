package com.byd.vehiclecontrol;

import android.util.Log;

/**
 * Простой тестовый процесс для проверки запуска
 */
public class TestProcess {
    private static final String TAG = "TestProcess";
    
    public static void main(String[] args) {
        System.out.println("[TestProcess] Starting...");
        Log.d(TAG, "TestProcess started with args: " + java.util.Arrays.toString(args));
        
        try {
            // Простой тест
            System.out.println("[TestProcess] Running...");
            
            // Держим процесс живым
            for (int i = 0; i < 10; i++) {
                System.out.println("[TestProcess] Alive: " + i);
                Thread.sleep(1000);
            }
            
            System.out.println("[TestProcess] Exiting normally");
            
        } catch (Exception e) {
            System.err.println("[TestProcess] Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.exit(0);
    }
}