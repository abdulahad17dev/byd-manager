package com.byd.vehiclecontrol.shell;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import java.lang.reflect.Method;

public class CommunicationBinder extends Binder {
    private static final String TAG = "CommunicationBinder";
    private static final String DESCRIPTOR = "VoiceAssistant";

    // Транзакции
    private static final int TRANSACTION_SET_INT = 20;
    private static final int TRANSACTION_START_VEHICLE = 21;
    private static final int TRANSACTION_NOTIFY_RECEIVED = 7;

    private final CommunicationProcess process;
    private static boolean binderReceived = false;
    
    // Глобальный экземпляр для доступа из того же процесса
    private static CommunicationBinder globalInstance;

    public CommunicationBinder(CommunicationProcess process) {
        this.process = process;
    }

    public static boolean isBinderReceived() {
        return binderReceived;
    }
    
    public static void setGlobalInstance(CommunicationBinder instance) {
        globalInstance = instance;
        Log.d(TAG, "Global instance set: " + (instance != null));
    }
    
    public static CommunicationBinder getGlobalInstance() {
        return globalInstance;
    }

    @Override
    public String getInterfaceDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case INTERFACE_TRANSACTION:
                reply.writeString(DESCRIPTOR);
                return true;

            case TRANSACTION_SET_INT:
                data.enforceInterface(DESCRIPTOR);
                return handleSetInt(data, reply);

            case TRANSACTION_START_VEHICLE:
                data.enforceInterface(DESCRIPTOR);
                return handleStartVehicle(data, reply);

            case TRANSACTION_NOTIFY_RECEIVED:
                data.enforceInterface(DESCRIPTOR);
                return handleNotifyReceived(data, reply);

            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    private boolean handleSetInt(Parcel data, Parcel reply) {
        try {
            // Читаем параметры
            int deviceType = data.readInt();
            int eventType = data.readInt();
            int value = data.readInt();

            Log.d(TAG, String.format("handleSetInt: device=%d, event=%d, value=%d",
                    deviceType, eventType, value));

            // Получаем auto service через кастомный Application
            Object autoService = process.getApplication().getSystemService("auto");

            if (autoService == null) {
                Log.e(TAG, "Auto service is null!");
                reply.writeException(new RuntimeException("Auto service not available"));
                return false;
            }

            // Вызываем setInt через рефлексию
            Method setIntMethod = autoService.getClass().getDeclaredMethod(
                    "setInt", Integer.TYPE, Integer.TYPE, Integer.TYPE
            );
            setIntMethod.setAccessible(true);

            Object result = setIntMethod.invoke(autoService, deviceType, eventType, value);
            int resultValue = result != null ? (Integer) result : 0;

            // КРИТИЧНО: сначала writeNoException, потом результат!
            reply.writeNoException();
            reply.writeInt(resultValue);

            Log.d(TAG, "setInt success, result: " + resultValue);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle setInt", e);

            // Если произошла ошибка, пишем исключение
            if (reply != null) {
                reply.writeException(e);
            }
            return false;
        }
    }

    private boolean handleStartVehicle(Parcel data, Parcel reply) {
        try {
            int deviceType = data.readInt();
            long time = data.readLong();
            int reason = data.readInt();
            String reasonStr = data.readString();

            Log.d(TAG, String.format("handleStartVehicle: device=%d, time=%d, reason=%d, str=%s",
                    deviceType, time, reason, reasonStr));

            Object autoService = process.getApplication().getSystemService("auto");

            if (autoService == null) {
                Log.e(TAG, "Auto service is null for startVehicle!");
                reply.writeException(new RuntimeException("Auto service not available"));
                return false;
            }

            // Вызываем startVehicle
            Method startVehicleMethod = autoService.getClass().getDeclaredMethod(
                    "startVehicle", Integer.TYPE, Long.TYPE, Integer.TYPE, String.class
            );
            startVehicleMethod.setAccessible(true);

            Object result = startVehicleMethod.invoke(autoService, deviceType, time, reason, reasonStr);
            int resultValue = result != null ? (Integer) result : 0;

            reply.writeNoException();
            reply.writeInt(resultValue);

            Log.d(TAG, "startVehicle success, result: " + resultValue);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle startVehicle", e);
            if (reply != null) {
                reply.writeException(e);
            }
            return false;
        }
    }

    private boolean handleNotifyReceived(Parcel data, Parcel reply) {
        try {
            binderReceived = true;
            Log.d(TAG, "Binder received notification");

            reply.writeNoException();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle notify", e);
            if (reply != null) {
                reply.writeException(e);
            }
            return false;
        }
    }

    // Статические методы для вызова из UI
    public static class Companion {

        public static int setInt(IBinder binder, int deviceType, int eventType, int value) {
            if (binder == null) return -1;

            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();

            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeInt(deviceType);
                data.writeInt(eventType);
                data.writeInt(value);

                boolean success = binder.transact(TRANSACTION_SET_INT, data, reply, 0);

                if (success) {
                    reply.readException(); // Читаем возможное исключение
                    return reply.readInt();
                }

                return -1;

            } catch (Exception e) {
                Log.e(TAG, "Failed to call setInt", e);
                return -1;
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        public static void notifyReceived(IBinder binder) {
            if (binder == null) return;

            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();

            try {
                data.writeInterfaceToken(DESCRIPTOR);
                binder.transact(TRANSACTION_NOTIFY_RECEIVED, data, reply, 0);
                reply.readException();
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify", e);
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}