package com.byd.vehiclecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.TextView;
import com.byd.vehiclecontrol.IVehicleControl;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements AdbShellHelper.ConnectionCallback {

    private Button btnConnect, btnDisconnect, btnEnableBt, btnDisableBt, btnCheckStatus;
    private Button btnStartAppProcess, btnStopAppProcess, btnCheckAppProcess;
    private TextView tvStatus, tvAppProcessStatus;
    
    // Новые кнопки для управления автомобилем
    private Button btnTestConnection, btnOpenWindow, btnCloseWindow, btnOpenAllWindows, btnCloseAllWindows;
    private Button btnDiscoverFunctions, btnSendCustomCommand;
    private TextView tvVehicleStatus;
    
    // AIDL Service для IPC
    private IVehicleControl vehicleControlService;
    private boolean isServiceBound = false;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            vehicleControlService = IVehicleControl.Stub.asInterface(service);
            isServiceBound = true;
            android.util.Log.d("MainActivity", "VehicleBinderService подключен");
            updateVehicleUI();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            vehicleControlService = null;
            isServiceBound = false;
            android.util.Log.d("MainActivity", "VehicleBinderService отключен");
            updateVehicleUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();

        // Устанавливаем callback для получения уведомлений о статусе соединения
        AdbShellHelper.setConnectionCallback(this);

        // Привязываемся к VehicleBinderService
        bindToVehicleService();

        // Сначала обновляем UI с текущим статусом
        updateUI();

        // Затем пытаемся автматически подключиться
        checkAndAutoConnect();
    }
    
    private void bindToVehicleService() {
        Intent intent = new Intent(this, VehicleBinderService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        android.util.Log.d("MainActivity", "Привязка к VehicleBinderService...");
    }

    private void checkAndAutoConnect() {
        // Показываем индикатор проверки
        tvStatus.setText("🔄 Проверка ADB...");
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));

        // Запускаем автоподключение
        AdbShellHelper.autoConnectOnStartup(getApplicationContext());

        // Через 3 секунды проверяем результат, если статус не изменился
        new android.os.Handler().postDelayed(() -> {
            if (!AdbShellHelper.isConnected()) {
                updateUI(); // Обновляем UI если подключение не удалось
            }
        }, 3000);
    }

    private void initViews() {
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnEnableBt = findViewById(R.id.btnEnableBt);
        btnDisableBt = findViewById(R.id.btnDisableBt);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        tvStatus = findViewById(R.id.tvStatus);

        // Новые элементы для app_process
        btnStartAppProcess = findViewById(R.id.btnStartAppProcess);
        btnStopAppProcess = findViewById(R.id.btnStopAppProcess);
        btnCheckAppProcess = findViewById(R.id.btnCheckAppProcess);
        tvAppProcessStatus = findViewById(R.id.tvAppProcessStatus);
        
        // Новые элементы для управления автомобилем
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnOpenWindow = findViewById(R.id.btnOpenWindow);
        btnCloseWindow = findViewById(R.id.btnCloseWindow);
        btnOpenAllWindows = findViewById(R.id.btnOpenAllWindows);
        btnCloseAllWindows = findViewById(R.id.btnCloseAllWindows);
        btnDiscoverFunctions = findViewById(R.id.btnDiscoverFunctions);
        btnSendCustomCommand = findViewById(R.id.btnSendCustomCommand);
        tvVehicleStatus = findViewById(R.id.tvVehicleStatus);
    }

    private void setupListeners() {
        btnConnect.setOnClickListener(v -> {
            btnConnect.setEnabled(false);
            btnConnect.setText("Подключение...");
            AdbShellHelper.connect(getApplicationContext());
        });

        btnDisconnect.setOnClickListener(v -> {
            AdbShellHelper.disconnect();
        });

        btnEnableBt.setOnClickListener(v -> {
            AdbShellHelper.runSingleCommand("svc bluetooth enable");
        });

        btnDisableBt.setOnClickListener(v -> {
            AdbShellHelper.runSingleCommand("svc bluetooth disable");
        });

        btnCheckStatus.setOnClickListener(v -> {
            AdbShellHelper.checkConnectionStatus();
        });

        // Новые обработчики для app_process
        btnStartAppProcess.setOnClickListener(v -> {
            btnStartAppProcess.setEnabled(false);
            btnStartAppProcess.setText("Запуск...");
            AdbShellHelper.startAppProcess(this);

            // Обновляем UI через 2 секунды
            new android.os.Handler().postDelayed(() -> {
                updateAppProcessUI();
            }, 2000);
        });

        btnStopAppProcess.setOnClickListener(v -> {
            AdbShellHelper.stopAppProcess();
            updateAppProcessUI();
        });

        btnCheckAppProcess.setOnClickListener(v -> {
            AdbShellHelper.checkAppProcessStatus();
        });
        
        // Обработчики для управления автомобилем
        setupVehicleControlListeners();
    }

    private void updateUI() {
        boolean connected = AdbShellHelper.isConnected();

        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        btnEnableBt.setEnabled(connected);
        btnDisableBt.setEnabled(connected);

        // Кнопки app_process доступны только при подключенном ADB
        btnStartAppProcess.setEnabled(connected && !AdbShellHelper.isAppProcessRunning());
        btnStopAppProcess.setEnabled(connected && AdbShellHelper.isAppProcessRunning());
        btnCheckAppProcess.setEnabled(connected);

        if (connected) {
            tvStatus.setText("🟢 ADB Подключено");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnConnect.setText("Подключено к ADB");
        } else {
            tvStatus.setText("🔴 ADB Отключено");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnConnect.setText("Подключиться к ADB");
        }

        updateAppProcessUI();
        updateVehicleUI();
    }

    private void updateAppProcessUI() {
        boolean connected = AdbShellHelper.isConnected();
        boolean appProcessRunning = AdbShellHelper.isAppProcessRunning();

        btnStartAppProcess.setEnabled(connected && !appProcessRunning);
        btnStopAppProcess.setEnabled(connected && appProcessRunning);
        btnCheckAppProcess.setEnabled(connected);

        if (!connected) {
            tvAppProcessStatus.setText("⚫ ADB не подключен");
            tvAppProcessStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            btnStartAppProcess.setText("Запустить app_process");
        } else if (appProcessRunning) {
            tvAppProcessStatus.setText("🟢 app_process Запущен");
            tvAppProcessStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnStartAppProcess.setText("app_process запущен");
        } else {
            tvAppProcessStatus.setText("🔴 app_process Остановлен");
            tvAppProcessStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnStartAppProcess.setText("Запустить app_process");
        }
    }

    // Callbacks от AdbShellHelper
    @Override
    public void onConnected() {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            updateUI();
            // Дополнительная обработка ошибок если нужно
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // При возвращении в активность проверяем статус и пытаемся переподключиться если нужно
        if (!AdbShellHelper.isConnected()) {
            checkAndAutoConnect();
        } else {
            updateUI();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Отвязываемся от сервиса
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
            android.util.Log.d("MainActivity", "Отвязка от VehicleBinderService");
        }
    }
    
    // ===== МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ АВТОМОБИЛЕМ =====
    
    private void setupVehicleControlListeners() {
        btnTestConnection.setOnClickListener(v -> testVehicleConnection());
        
        btnOpenWindow.setOnClickListener(v -> {
//            btnOpenWindow.setEnabled(false);
            btnOpenWindow.setText("Открываем...");
            
            new Thread(() -> {
                boolean success = false;
                if (isServiceBound && vehicleControlService != null) {
                    try {
                        // Используем AIDL сервис для отправки команды
                        int result = vehicleControlService.sendCommand(1001, 1125122104, 1);
                        success = result >= 0;
                    } catch (RemoteException e) {
                        android.util.Log.e("MainActivity", "Ошибка вызова сервиса", e);
                    }
                } else {
                    android.util.Log.w("MainActivity", "Сервис не подключен");
                }
                
                final boolean finalSuccess = success;
                runOnUiThread(() -> {
//                    btnOpenWindow.setEnabled(true);
                    btnOpenWindow.setText("Открыть окно");
                    updateVehicleStatus("Переднее левое окно: " + (finalSuccess ? "открыто ✅" : "ошибка ❌"));
                });
            }).start();
        });
        
        btnCloseWindow.setOnClickListener(v -> {
//            btnCloseWindow.setEnabled(false);
            btnCloseWindow.setText("Закрываем...");
            
            new Thread(() -> {
                boolean success = false;
                if (isServiceBound && vehicleControlService != null) {
                    try {
                        // Используем AIDL сервис для отправки команды
                        int result = vehicleControlService.sendCommand(1001, 1125122104, 2);
                        success = result >= 0;
                    } catch (RemoteException e) {
                        android.util.Log.e("MainActivity", "Ошибка вызова сервиса", e);
                    }
                } else {
                    android.util.Log.w("MainActivity", "Сервис не подключен");
                }
                
                final boolean finalSuccess = success;
                runOnUiThread(() -> {
//                    btnCloseWindow.setEnabled(true);
                    btnCloseWindow.setText("Закрыть окно");
                    updateVehicleStatus("Переднее левое окно: " + (finalSuccess ? "закрыто ✅" : "ошибка ❌"));
                });
            }).start();
        });
        
        btnOpenAllWindows.setOnClickListener(v -> {
            btnOpenAllWindows.setEnabled(false);
            btnOpenAllWindows.setText("Открываем все...");
            
            new Thread(() -> {
                boolean success = VehicleControlHelper.openAllWindows();
                
                runOnUiThread(() -> {
                    btnOpenAllWindows.setEnabled(true);
                    btnOpenAllWindows.setText("Открыть все окна");
                    updateVehicleStatus("Все окна: " + (success ? "открыты ✅" : "ошибка ❌"));
                });
            }).start();
        });
        
        btnCloseAllWindows.setOnClickListener(v -> {
            btnCloseAllWindows.setEnabled(false);
            btnCloseAllWindows.setText("Закрываем все...");
            
            new Thread(() -> {
                boolean success = VehicleControlHelper.closeAllWindows();
                
                runOnUiThread(() -> {
                    btnCloseAllWindows.setEnabled(true);
                    btnCloseAllWindows.setText("Закрыть все окна");
                    updateVehicleStatus("Все окна: " + (success ? "закрыты ✅" : "ошибка ❌"));
                });
            }).start();
        });
        
        btnDiscoverFunctions.setOnClickListener(v -> {
            btnDiscoverFunctions.setEnabled(false);
            btnDiscoverFunctions.setText("Сканируем...");
            updateVehicleStatus("🔍 Поиск доступных функций...");
            
            new Thread(() -> {
                VehicleControlHelper.discoverVehicleFunctions();
                
                runOnUiThread(() -> {
                    btnDiscoverFunctions.setEnabled(true);
                    btnDiscoverFunctions.setText("Поиск функций");
                    updateVehicleStatus("Поиск функций завершен. Смотрите логи.");
                });
            }).start();
        });
        
        btnSendCustomCommand.setOnClickListener(v -> sendCustomCommand());
    }
    
    private void testVehicleConnection() {
        btnTestConnection.setEnabled(false);
        btnTestConnection.setText("Тестируем...");
        updateVehicleStatus("🔄 Тестируем связь с автомобилем...");
        
        new Thread(() -> {
            boolean available = false;
            boolean testResult = false;
            
            if (isServiceBound && vehicleControlService != null) {
                try {
                    available = vehicleControlService.isConnected();
                    if (available) {
                        // Тестовая команда
                        int result = vehicleControlService.sendCommand(0, 0, 0);
                        testResult = result >= 0;
                    }
                } catch (RemoteException e) {
                    android.util.Log.e("MainActivity", "Ошибка теста соединения", e);
                }
            }
            
            // Используем final переменные для лямбда
            final boolean isAvailable = available;
            final boolean finalTestResult = testResult;
            
            runOnUiThread(() -> {
                btnTestConnection.setEnabled(true);
                btnTestConnection.setText("Тест связи");
                
                if (!isAvailable) {
                    updateVehicleStatus("❌ Связь недоступна. Запустите app_process.");
                } else if (finalTestResult) {
                    updateVehicleStatus("✅ Связь с автомобилем работает!");
                } else {
                    updateVehicleStatus("⚠️ Binder доступен, но команды не проходят");
                }
                
                updateVehicleUI();
            });
        }).start();
    }
    
    private void sendCustomCommand() {
        // Простой диалог для ввода параметров
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        
        // Создаем простые поля ввода
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        android.widget.EditText etDeviceType = new android.widget.EditText(this);
        etDeviceType.setHint("Device Type (например: 1)");
        etDeviceType.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        android.widget.EditText etEventType = new android.widget.EditText(this);
        etEventType.setHint("Event Type (например: 2)");
        etEventType.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        android.widget.EditText etValue = new android.widget.EditText(this);
        etValue.setHint("Value (например: 1)");
        etValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        layout.addView(etDeviceType);
        layout.addView(etEventType);
        layout.addView(etValue);
        
        builder.setTitle("Отправить команду")
               .setView(layout)
               .setPositiveButton("Отправить", (dialog, which) -> {
                   try {
                       int deviceType = Integer.parseInt(etDeviceType.getText().toString());
                       int eventType = Integer.parseInt(etEventType.getText().toString());
                       int value = Integer.parseInt(etValue.getText().toString());
                       
                       new Thread(() -> {
                           int result = VehicleControlHelper.sendSetIntCommand(deviceType, eventType, value);
                           
                           runOnUiThread(() -> {
                               updateVehicleStatus(String.format("Команда (%d,%d,%d) - результат: %d %s", 
                                       deviceType, eventType, value, result, 
                                       result >= 0 ? "✅" : "❌"));
                           });
                       }).start();
                       
                   } catch (NumberFormatException e) {
                       updateVehicleStatus("❌ Ошибка: введите числа");
                   }
               })
               .setNegativeButton("Отмена", null)
               .show();
    }
    
    private void updateVehicleStatus(String status) {
        if (tvVehicleStatus != null) {
            tvVehicleStatus.setText(status);
        }
        android.util.Log.d("MainActivity", "Vehicle status: " + status);
    }
    
    private void updateVehicleUI() {
        // Проверяем что все кнопки инициализированы
        if (btnTestConnection == null || btnOpenWindow == null || btnCloseWindow == null ||
            btnOpenAllWindows == null || btnCloseAllWindows == null || 
            btnDiscoverFunctions == null || btnSendCustomCommand == null) {
            // Если кнопки еще не инициализированы, выходим
            return;
        }
        
        boolean appProcessRunning = AdbShellHelper.isAppProcessRunning();
        boolean vehicleAvailable = false;
        
        if (isServiceBound && vehicleControlService != null) {
            try {
                vehicleAvailable = vehicleControlService.isConnected();
            } catch (RemoteException e) {
                android.util.Log.e("MainActivity", "Ошибка проверки соединения", e);
            }
        }
        
        // Кнопки управления доступны только при запущенном app_process и доступном Binder
        boolean enableVehicleControls = appProcessRunning && vehicleAvailable;
        
//        btnTestConnection.setEnabled(appProcessRunning);
//        btnOpenWindow.setEnabled(enableVehicleControls);
//        btnCloseWindow.setEnabled(enableVehicleControls);
//        btnOpenAllWindows.setEnabled(enableVehicleControls);
//        btnCloseAllWindows.setEnabled(enableVehicleControls);
//        btnDiscoverFunctions.setEnabled(enableVehicleControls);
//        btnSendCustomCommand.setEnabled(enableVehicleControls);
        
        // Обновляем статус
        if (!appProcessRunning) {
            updateVehicleStatus("⚫ Для управления нужен app_process");
        } else if (!vehicleAvailable) {
            updateVehicleStatus("🔄 Ожидание подключения к автомобилю...");
        } else {
            updateVehicleStatus("🟢 Готов к управлению автомобилем");
        }
    }
}