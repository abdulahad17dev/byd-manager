package com.byd.vehiclecontrol;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements AdbShellHelper.ConnectionCallback {

    private Button btnConnect, btnDisconnect, btnEnableBt, btnDisableBt, btnCheckStatus;
    private Button btnStartAppProcess, btnStopAppProcess, btnCheckAppProcess;
    private TextView tvStatus, tvAppProcessStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();

        // Устанавливаем callback для получения уведомлений о статусе соединения
        AdbShellHelper.setConnectionCallback(this);

        // Сначала обновляем UI с текущим статусом
        updateUI();

        // Затем пытаемся автomatически подключиться
        checkAndAutoConnect();
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
}