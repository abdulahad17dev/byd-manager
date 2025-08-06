package com.byd.vehiclecontrol;

/**
 * AIDL интерфейс для управления автомобилем
 */
interface IVehicleControl {
    /**
     * Отправляет команду управления
     */
    int sendCommand(int deviceType, int eventType, int value);
    
    /**
     * Проверяет доступность соединения
     */
    boolean isConnected();
}