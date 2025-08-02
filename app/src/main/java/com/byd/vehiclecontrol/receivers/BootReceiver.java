package com.byd.vehiclecontrol.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.byd.vehiclecontrol.utils.AdbUtil;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting communication process");
            startCommunicationProcess(context);
        }
    }

    private void startCommunicationProcess(Context context) {
        try {
            // Сначала убиваем старый процесс
            String killCommand = "kill -9 $(ps -A | grep VehicleControlProcess | awk 'NR==1{print $2}')";

            // Запускаем новый процесс через ADB с системными привилегиями
            String startCommand = "nohup app_process -Djava.class.path=" +
                    context.getPackageResourcePath() +
                    " --nice-name=VehicleControlProcess " +
                    "com.byd.vehiclecontrol.shell.CommunicationProcessMain " +
                    ">/dev/null 2>&1 &";

            // Выполняем команды через ADB
            AdbUtil.runShellCommandOnce(context, killCommand, startCommand);

            Log.d(TAG, "Communication process started via ADB");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start communication process", e);
        }
    }
}
