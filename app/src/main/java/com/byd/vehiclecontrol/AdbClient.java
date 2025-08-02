package com.byd.vehiclecontrol;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.MessageDigest;  // ДОБАВЛЕН ИМПОРТ
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.concurrent.TimeUnit;

public class AdbClient implements AutoCloseable {

    // Константы ADB протокола
    private static final int CMD_AUTHORIZATION = 1213486401;  // AUTH
    private static final int CMD_CLOSE = 1163086915;          // CLSE
    private static final int CMD_CONNECTION = 1314410051;     // CNXN
    private static final int CMD_OKAY = 1497451343;           // OKAY
    private static final int CMD_WRITE = 1163154007;          // WRTE

    // Таймауты
    private static final long CONNECT_TIMEOUT_MS = 10000;
    private static final long READ_TIMEOUT_MS = 8000;

    // RSA приватный ключ из декомпилированной версии
    private static final byte[] PRIVATE_KEY_DATA = {
            48, -126, 4, -66, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 4, -88, 48, -126, 4, -92, 2, 1, 0, 2, -126, 1, 1, 0, -97, -48, 71, -115, 36, 51, 83, -48, 3, -2, -96, 125, -38, -128, 53, 96, -69, -96, -23, -26, -35, -124, -88, 103, -80, 104, -75, -114, 36, -86, 27, 77, 64, -94, 123, -40, -127, 34, -67, -49, -91, -37, -9, 32, -37, 122, 90, 31, 112, -102, -7, -100, -113, 74, -36, -116, -32, -108, -76, 106, -88, -6, -83, 38, 114, -77, -21, 12, -88, -93, -105, 42, 45, -24, -113, 16, -91, -15, 52, -38, 52, 2, -25, 38, -45, 15, 77, -86, 14, -102, -77, 5, 35, 84, -14, -64, 88, 42, -6, 88, -101, -18, -11, 8, 56, 91, 17, -62, 108, -109, 2, 77, -10, -19, 125, 6, -1, -93, -127, -34, 46, -64, -126, 26, 29, 123, 30, 78, -54, -66, -81, 112, -124, 64, -14, -35, -33, 3, -54, 53, -68, -30, 77, -4, -81, 91, -63, -25, -68, 62, -124, 109, -99, 109, -81, 49, 50, 90, 97, -23, -43, -77, 108, -78, -92, 12, -80, -84, -28, -97, 126, -19, 116, -81, 88, 15, -42, 117, 27, -113, -86, 10, -10, 126, -67, 97, 58, -123, 79, 22, -94, -83, 105, 0, -34, -81, -115, -56, -87, -9, 92, -51, -123, 109, -67, -16, -108, -105, 13, 21, 19, -23, -76, -110, -93, 4, -63, -21, -37, 49, -109, -61, 53, 29, -57, 81, -24, 95, -11, -92, 62, -74, 111, -125, 49, 45, 34, 93, 83, -10, 122, 106, -35, -107, 59, 86, -76, 73, 24, 33, 43, -109, 108, 31, 59, -91, 2, 3, 1, 0, 1, 2, -126, 1, 0, 0, -69, -113, -92, 57, 93, 122, 4, -120, 60, -70, -3, 27, -65, 6, 27, 13, 29, 89, 24, -6, 127, -97, -91, -116, 36, 120, 69, -63, 1, 83, 29, -121, 0, -7, 55, -62, 12, 46, -101, 126, 55, 73, 16, 77, 109, 122, 0, -47, -76, -103, -28, -32, -106, -46, 14, -91, 72, 67, -23, 25, 44, -53, 80, -87, 6, 1, -27, -31, 93, 22, -46, -46, -3, 75, 66, -51, -69, 0, 49, 98, -38, 21, -53, 15, -77, -115, 84, -85, 95, -38, 11, 89, 87, -50, -13, 121, -54, -37, -30, -23, 3, -121, 69, 13, 54, 10, -48, -118, 3, 56, -74, -66, -67, -77, -41, -83, 125, -118, -121, 23, 115, 16, 112, 75, -81, -8, -104, 96, -70, -96, 123, -39, -16, 82, 79, 21, -94, -38, 12, -89, 65, -111, -104, 74, -59, 120, -16, 114, -84, -14, -81, 124, -39, -6, 43, -127, 100, -51, -114, -42, -102, 86, 62, 49, -14, 4, -112, -45, -15, 83, 125, -87, -46, -91, 93, 46, 106, -98, 49, -95, 114, 28, -10, 48, -40, 29, -47, 60, 52, 72, -112, 23, 60, 33, -114, -61, 126, -120, 117, 36, 123, -42, 10, 54, -21, -99, 52, -105, 10, 93, -28, 56, -6, -39, 36, -39, 27, -48, -66, 82, -116, -53, 65, 122, 110, -12, 109, -88, 22, -96, 85, -48, -88, -114, -49, -78, -7, 29, -117, 27, -104, 108, -24, 52, -66, 28, 17, -45, -110, -104, -55, -81, -61, -22, 11, 2, -127, -127, 0, -33, 56, 16, 4, 13, 39, 101, 58, 44, -112, 101, 70, -35, 66, 74, -69, 110, 108, -11, 28, -79, 12, -67, 25, -92, 101, 56, -68, 87, -68, 23, -35, 65, 51, 22, -21, 113, -127, -126, 71, 20, -65, -75, -25, -96, 105, 92, -67, -10, 45, -88, -103, -120, 56, 0, -88, -38, -42, -127, 68, -23, 71, 39, -36, -80, -10, -114, -126, -75, 12, -28, -52, -40, 26, -108, 97, -7, 10, 28, -23, 119, 103, 16, -109, 20, 118, 106, -48, 126, 76, -30, -96, -22, 25, 31, 34, -54, 126, 91, 106, 48, 30, -21, -98, -31, -60, 112, -21, 32, 117, -33, -51, -66, -81, -99, -9, 64, 65, 110, 34, -67, 123, -33, 71, 66, -80, 65, 3, 2, -127, -127, 0, -73, 72, 124, 62, -102, 12, -128, 42, -7, -27, 1, 66, 101, -32, 125, 86, -111, -3, 14, -43, 121, -118, -48, 120, -87, 23, -108, 23, 13, -41, -9, 35, 88, -49, 97, -31, -27, -109, -110, 14, 120, 73, 69, -48, -11, 66, -114, 108, -46, 79, 104, -81, -10, 108, -12, -92, -79, -3, 40, -48, 80, -115, -29, 76, 101, -49, -88, 115, -81, 71, 46, -106, -31, 68, 18, -15, 89, 82, -79, 51, 53, 38, -83, 42, 47, 96, 107, -127, 51, 43, -55, 82, 59, -59, -51, 10, 83, 88, -106, -123, -116, -10, -95, -121, -55, 59, -87, -87, -105, -31, 116, 127, 19, -29, -31, -119, 77, 123, -82, 8, 79, 74, 32, 65, -75, -100, 108, 55, 2, -127, -127, 0, -91, 110, -52, 87, -28, 83, -51, 47, 23, 54, 17, 9, 59, 20, 85, -124, -95, -21, 120, -95, -62, 9, -7, -32, 22, 57, -70, -103, -61, -64, 48, 67, -105, 125, -64, -65, -48, 8, -74, -65, -19, 125, -61, -40, 29, -57, -40, -89, 36, -37, 99, -8, 29, -65, -69, -91, 105, 66, -50, -35, 126, -78, 112, -75, -100, 37, -81, 42, -23, -7, -37, 92, -127, -48, -7, 37, -65, 71, -94, 115, -39, 61, 118, 72, 59, 67, 36, 24, -49, 114, 54, 8, 34, 87, 105, -1, 3, -22, 47, -33, -4, 55, -2, 82, 107, 106, -122, 113, -116, 70, 48, 15, 49, 2, -64, -27, 45, 39, 16, -12, 8, 80, -43, 2, -32, 70, -10, 119, 2, -127, -128, 35, -2, 34, 24, 17, 28, 127, 86, -15, 56, 29, -91, 50, 104, -127, 116, -84, -107, 91, -114, 100, -115, -12, 30, -99, -26, 57, 120, -59, -119, 49, -55, 73, 57, -128, -103, 98, 2, 54, -34, -116, -108, -89, 23, -3, -14, -48, 17, 98, -61, -95, 101, 92, -39, 76, -71, -62, -19, 10, 80, -50, 96, -18, -48, 35, -10, 65, -72, 102, 37, 110, 106, -58, -42, 29, 122, 51, -10, 95, 66, 21, 32, 1, 69, -107, -124, 51, -40, 109, 122, 29, -48, -2, 87, -5, -54, 25, 79, -2, 79, -3, -67, 119, -3, 57, 47, 116, 68, 15, -59, -128, -95, 44, 0, -58, 91, -74, 81, -95, 125, -108, 68, -108, 77, 19, 35, 34, -59, 2, -127, -127, 0, -100, 86, 105, -124, 16, -3, -77, 55, 97, 100, -76, -19, 93, -122, -55, -120, 38, 125, -127, 94, -34, 73, 105, -40, 59, -16, 100, -120, -53, -111, 58, 71, -109, 71, 12, -56, -119, 116, -104, 15, -49, -85, 19, -69, -99, -65, -7, 75, -104, 109, 126, 96, -54, -21, -111, -79, -93, -104, -48, 31, 107, -106, 81, -118, -57, -60, -128, -116, 112, 3, -42, -79, 68, -62, -3, -36, 10, -18, -81, -57, 32, 117, 54, -43, 57, 10, -127, -120, 96, 101, -97, -45, 114, 41, -75, 69, 32, -90, 86, -7, 25, 124, 43, -98, -116, 23, 54, 110, 87, 80, -45, 75, 65, -48, -51, -19, -95, -34, 44, 7, -7, 22, 107, 17, -121, 96, -39, -32
    };

    // Публичный ключ для авторизации из декомпилированной версии
    private static final byte[] PUBLIC_KEY_AUTHORIZATION_DATA = {
            81, 65, 65, 65, 65, 78, 80, 76, 80, 106, 121, 108, 79, 120, 57, 115, 107, 121, 115, 104, 71, 69, 109, 48, 86, 106, 117, 86, 51, 87, 112, 54, 57, 108, 78, 100, 73, 105, 48, 120, 103, 50, 43, 50, 80, 113, 84, 49, 88, 43, 104, 82, 120, 120, 48, 49, 119, 53, 77, 120, 50, 43, 118, 66, 66, 75, 79, 83, 116, 79, 107, 84, 70, 81, 50, 88, 108, 80, 67, 57, 98, 89, 88, 78, 88, 80, 101, 112, 121, 73, 50, 118, 51, 103, 66, 112, 114, 97, 73, 87, 84, 52, 85, 54, 89, 98, 49, 43, 57, 103, 113, 113, 106, 120, 116, 49, 49, 103, 57, 89, 114, 51, 84, 116, 102, 112, 47, 107, 114, 76, 65, 77, 112, 76, 74, 115, 115, 57, 88, 112, 89, 86, 111, 121, 77, 97, 57, 116, 110, 87, 50, 69, 80, 114, 122, 110, 119, 86, 117, 118, 47, 69, 51, 105, 118, 68, 88, 75, 65, 57, 47, 100, 56, 107, 67, 69, 99, 75, 43, 43, 121, 107, 52, 101, 101, 120, 48, 97, 103, 115, 65, 117, 51, 111, 71, 106, 47, 119, 90, 57, 55, 102, 90, 78, 65, 112, 78, 115, 119, 104, 70, 98, 79, 65, 106, 49, 55, 112, 116, 89, 43, 105, 112, 89, 119, 80, 74, 85, 73, 119, 87, 122, 109, 103, 54, 113, 84, 81, 47, 84, 74, 117, 99, 67, 78, 78, 111, 48, 56, 97, 85, 81, 106, 43, 103, 116, 75, 112, 101, 106, 113, 65, 122, 114, 115, 51, 73, 109, 114, 102, 113, 111, 97, 114, 83, 85, 52, 73, 122, 99, 83, 111, 43, 99, 43, 90, 112, 119, 72, 49, 112, 54, 50, 121, 68, 51, 50, 54, 88, 80, 118, 83, 75, 66, 50, 72, 117, 105, 81, 69, 48, 98, 113, 105, 83, 79, 116, 87, 105, 119, 90, 54, 105, 69, 51, 101, 98, 112, 111, 76, 116, 103, 78, 89, 68, 97, 102, 97, 68, 43, 65, 57, 66, 84, 77, 121, 83, 78, 82, 57, 67, 102, 101, 55, 86, 53, 110, 72, 89, 119, 85, 90, 71, 68, 57, 99, 87, 53, 82, 51, 56, 121, 120, 82, 65, 68, 48, 111, 69, 69, 81, 68, 57, 120, 79, 73, 122, 47, 122, 97, 103, 81, 65, 47, 106, 98, 103, 70, 100, 83, 103, 74, 118, 90, 52, 71, 88, 99, 88, 77, 116, 90, 88, 85, 105, 49, 122, 121, 68, 66, 73, 67, 97, 85, 51, 101, 108, 117, 48, 67, 71, 88, 75, 84, 104, 117, 76, 82, 106, 71, 49, 109, 80, 122, 83, 50, 86, 51, 101, 116, 90, 78, 101, 82, 117, 75, 103, 69, 103, 122, 57, 113, 75, 69, 101, 68, 106, 83, 81, 119, 119, 74, 90, 76, 54, 97, 78, 82, 65, 106, 87, 51, 50, 104, 97, 66, 115, 106, 114, 115, 111, 117, 110, 88, 65, 109, 104, 115, 101, 110, 109, 101, 103, 53, 112, 103, 122, 120, 108, 72, 111, 57, 47, 83, 57, 57, 98, 120, 43, 47, 115, 117, 110, 115, 72, 71, 117, 112, 81, 87, 82, 71, 79, 106, 81, 97, 76, 69, 116, 77, 70, 122, 111, 118, 53, 89, 104, 118, 111, 120, 49, 90, 83, 107, 52, 57, 66, 43, 98, 102, 119, 43, 77, 72, 53, 48, 77, 87, 118, 49, 43, 70, 48, 47, 89, 78, 71, 51, 53, 75, 67, 112, 104, 79, 88, 48, 83, 77, 74, 97, 83, 99, 113, 56, 103, 100, 109, 43, 99, 122, 76, 90, 117, 53, 84, 78, 79, 51, 113, 52, 66, 89, 72, 68, 97, 112, 50, 115, 102, 47, 71, 73, 43, 112, 57, 50, 109, 86, 88, 121, 56, 54, 112, 106, 117, 106, 73, 117, 120, 119, 119, 120, 107, 120, 116, 109, 81, 48, 49, 71, 52, 79, 103, 52, 121, 101, 113, 121, 51, 75, 120, 85, 89, 73, 90, 87, 50, 115, 47, 114, 87, 115, 109, 69, 99, 50, 86, 106, 100, 50, 85, 97, 70, 105, 85, 112, 82, 99, 78, 72, 69, 87, 71, 81, 69, 65, 65, 81, 65, 61, 32, 119, 105, 114, 101, 108, 101, 115, 115, 64, 97, 100, 98, 0
    };

    // Предопределенные сообщения протокола
    private static final byte[] MSG_CONNECT = {
            67, 78, 88, 78, 0, 0, 0, 1, 0, 16, 0, 0, 7, 0, 0, 0, 50, 2, 0, 0, -68, -79, -89, -79, 104, 111, 115,
            116, 58, 58, 0
    };

    private static final byte[] MSG_OPEN = {
            79, 80, 69, 78, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 82, 2, 0, 0, -80, -81, -70, -79, 115, 104, 101,
            108, 108, 58, 0
    };

    // Состояние соединения
    private Socket socket;
    private DataInputStream inputStream;
    private OutputStream outputStream;
    private int localId = 1;
    private int remoteId;
    private boolean connected = false;
    private static PrivateKey privateKey;

    // Инициализация ключей
    static {
        try {
            privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(PRIVATE_KEY_DATA));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RSA key", e);
        }
    }

    private AdbClient(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout((int) READ_TIMEOUT_MS);

        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = socket.getOutputStream();
    }

    public static AdbClient connectShell(String host, int port, long timeoutMs, int maxRetries)
            throws IOException, InterruptedException {

        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                AdbClient client = new AdbClient(host, port);

                if (client.performHandshake()) {
                    client.openShell();
                    client.connected = true;
                    return client;
                }

                client.close();

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000);
                }
            }
        }

        throw new IOException("Failed to connect after " + maxRetries + " attempts", lastException);
    }

    public static AdbClient connectShell(String host, int port) throws IOException, InterruptedException {
        return connectShell(host, port, CONNECT_TIMEOUT_MS, 3);
    }

    private boolean performHandshake() throws IOException {
        try {
            Log.d("AdbClient", "Starting handshake...");

            writeMessage(MSG_CONNECT.clone());
            AdbMessage response = readMessage();

            Log.d("AdbClient", "Received command: " + Integer.toHexString(response.command));

            if (response.command == CMD_AUTHORIZATION) {
                Log.d("AdbClient", "Device requests authorization immediately");

                // ИСПРАВЛЕННАЯ подпись
                byte[] signature = signByPrivateKey(response.data);
                Log.d("AdbClient", "Generated signature, length: " + signature.length);

                byte[] authMessage = generateMessage(CMD_AUTHORIZATION, 2, 0, signature);
                writeToSocket(authMessage);

                byte[] pubKeyMessage = generateMessage(CMD_AUTHORIZATION, 3, 0, PUBLIC_KEY_AUTHORIZATION_DATA);
                writeToSocket(pubKeyMessage);

                response = readMessage();
                Log.d("AdbClient", "Final response command: " + Integer.toHexString(response.command));

                boolean success = response.command == CMD_CONNECTION;
                Log.d("AdbClient", "Handshake " + (success ? "SUCCESS" : "FAILED"));
                return success;

            } else if (response.command == CMD_CONNECTION) {
                Log.d("AdbClient", "Device confirmed connection, proceeding with auth");

                byte[] signature = signByPrivateKey(response.data);
                byte[] authMessage = generateMessage(CMD_AUTHORIZATION, 2, 0, signature);
                writeToSocket(authMessage);

                byte[] pubKeyMessage = generateMessage(CMD_AUTHORIZATION, 3, 0, PUBLIC_KEY_AUTHORIZATION_DATA);
                writeToSocket(pubKeyMessage);

                response = readMessage();
                return response.command == CMD_CONNECTION;

            } else {
                Log.e("AdbClient", "Unexpected response command: " + Integer.toHexString(response.command));
                return false;
            }

        } catch (Exception e) {
            Log.e("AdbClient", "Handshake exception", e);
            throw new IOException("Handshake failed", e);
        }
    }

    // ИСПРАВЛЕННЫЙ МЕТОД ПОДПИСИ
    private byte[] signByPrivateKey(byte[] data) throws Exception {
        try {
            // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ 1: Хешируем данных с SHA-1
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(data);

            Log.d("AdbClient", "Original data length: " + data.length);
            Log.d("AdbClient", "SHA-1 hash length: " + hash.length);

            // ИСПРАВЛЕНИЕ 2: NoPadding cipher
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);

            // ИСПРАВЛЕНИЕ 3: Правильный padding для RSA-2048
            byte[] paddedData = new byte[256];

            paddedData[0] = 0x00;  // Leading zero
            paddedData[1] = 0x01;  // Block type 01

            // Fill with 0xFF bytes
            for (int i = 2; i < 256 - 35; i++) {
                paddedData[i] = (byte) 0xFF;
            }

            paddedData[256 - 35] = 0x00;  // Separator

            // ASN.1 DigestInfo for SHA-1
            byte[] digestInfo = {
                    0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
                    0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
            };

            System.arraycopy(digestInfo, 0, paddedData, 256 - 35 + 1, digestInfo.length);
            System.arraycopy(hash, 0, paddedData, 256 - 20, hash.length);

            Log.d("AdbClient", "Padded data ready, signing...");

            // ИСПРАВЛЕНИЕ 4: Прямая подпись
            return cipher.doFinal(paddedData);

        } catch (Exception e) {
            Log.e("AdbClient", "Signature failed", e);
            throw e;
        }
    }

    private void openShell() throws IOException {
        byte[] openMsg = MSG_OPEN.clone();
        ByteBuffer.wrap(openMsg, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(localId);
        writeToSocket(openMsg);

        AdbMessage response = readMessage();
        if (response.command == CMD_OKAY) {
            AdbMessage nextResponse = readMessage();
            remoteId = nextResponse.arg1;

            byte[] okayMsg = generateMessage(CMD_OKAY, localId, remoteId, new byte[0]);
            writeToSocket(okayMsg);
        } else {
            throw new IOException("Failed to open shell");
        }
    }

    public String executeCommand(String command) throws IOException {
        return executeCommand(command, true);
    }

    public String executeCommand(String command, boolean waitForResponse) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }

        StringBuilder result = new StringBuilder();

        byte[] commandBytes = (command + "\n").getBytes("UTF-8");
        byte[] writeMsg = generateMessage(CMD_WRITE, localId, remoteId, commandBytes);
        writeToSocket(writeMsg);

        AdbMessage response = readMessage();
        if (response.command != CMD_OKAY) {
            throw new IOException("Command execution failed");
        }

        if (waitForResponse) {
            while (true) {
                AdbMessage dataResponse = readMessage();
                remoteId = dataResponse.arg1;

                String output = new String(dataResponse.data, "UTF-8");
                result.append(output);

                byte[] okayMsg = generateMessage(CMD_OKAY, localId, remoteId, new byte[0]);
                writeToSocket(okayMsg);

                if (output.endsWith(" $ ") && inputStream.available() <= 0) {
                    break;
                }
            }
        }

        return result.toString();
    }

    public static boolean isAdbAvailable(String host, int port) {
        try (Socket testSocket = new Socket()) {
            testSocket.connect(new InetSocketAddress(host, port), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private byte[] generateMessage(int cmd, int arg0, int arg1, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 24).order(ByteOrder.LITTLE_ENDIAN);

        int checksum = 0;
        for (byte b : data) {
            checksum += (b & 0xFF);
        }

        buffer.putInt(cmd)
                .putInt(arg0)
                .putInt(arg1)
                .putInt(data.length)
                .putInt(checksum)
                .putInt(~cmd)
                .put(data);

        return buffer.array();
    }

    private AdbMessage readMessage() throws IOException {
        byte[] header = new byte[24];
        inputStream.readFully(header);

        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int command = buffer.getInt();
        int arg0 = buffer.getInt();
        int arg1 = buffer.getInt();
        int dataLength = buffer.getInt();
        int dataChecksum = buffer.getInt();
        int magic = buffer.getInt();

        if (command != (~magic)) {
            throw new IOException("Invalid magic number in response");
        }

        byte[] data = new byte[0];
        if (dataLength > 0) {
            data = new byte[dataLength];
            inputStream.readFully(data);

            int actualChecksum = 0;
            for (byte b : data) {
                actualChecksum += (b & 0xFF);
            }

            if (actualChecksum != dataChecksum) {
                throw new IOException("Data checksum mismatch");
            }
        }

        return new AdbMessage(command, arg0, arg1, data);
    }

    private synchronized void writeToSocket(byte[] data) throws IOException {
        outputStream.write(data);
        outputStream.flush();
    }

    private void writeMessage(byte[] message) throws IOException {
        writeToSocket(message);
    }

    @Override
    public void close() throws IOException {
        connected = false;

        if (socket != null && !socket.isClosed()) {
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                // Игнорируем ошибки при закрытии
            }
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    private static class AdbMessage {
        final int command;
        final int arg0;
        final int arg1;
        final byte[] data;

        AdbMessage(int command, int arg0, int arg1, byte[] data) {
            this.command = command;
            this.arg0 = arg0;
            this.arg1 = arg1;
            this.data = data;
        }
    }
}