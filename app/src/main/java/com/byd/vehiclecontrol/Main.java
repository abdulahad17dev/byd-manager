package com.byd.vehiclecontrol;

public class Main {
    public static void main(String[] args) {
        System.out.println("[Main] Starting shell process directly...");

        try {
            TestShellProcess process = new TestShellProcess();
            process.run(args);
        } catch (Exception e) {
            System.err.println("[Main] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}