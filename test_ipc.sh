#!/bin/sh
# Скрипт для тестирования IPC между процессами

echo "=== Тест IPC для BYDManager ==="

# 1. Проверяем ADB
echo "[1] Проверка ADB подключения..."
adb devices

# 2. Останавливаем старый процесс
echo "[2] Остановка старого процесса..."
adb shell "ps | grep VoiceAssistant" 
adb shell "pkill -f VoiceAssistantProcess"
sleep 1

# 3. Запускаем приложение
echo "[3] Запуск приложения..."
adb shell am start -n com.byd.vehiclecontrol/.MainActivity

# 4. Ждем инициализации
echo "[4] Ожидание инициализации..."
sleep 3

# 5. Запускаем shell процесс
echo "[5] Запуск shell процесса..."
adb shell "app_process -Djava.class.path=/data/app/*/com.byd.vehiclecontrol*/base.apk /data/data/com.byd.vehiclecontrol/cache --nice-name=VoiceAssistantProcess com.byd.vehiclecontrol.VoiceAssistantProcess &"

# 6. Проверяем запущенные процессы
echo "[6] Проверка процессов..."
sleep 2
adb shell "ps | grep -E 'vehiclecontrol|VoiceAssistant'"

# 7. Смотрим логи
echo "[7] Логи IPC..."
adb logcat -d | grep -E "CommunicationBinder|VehicleApplication|VoiceAssistantProcess|MainActivity" | tail -50

echo "=== Тест завершен ==="
echo "Проверьте, что кнопки управления стали активными в приложении"