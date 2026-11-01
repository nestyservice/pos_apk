package com.nesty.pos.printer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.nesty.pos.api.models.OrderModels;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class Model1008PrinterDriver implements IPrinterDriver {
    private static final String TAG = "Model1008Printer";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private boolean isConnected = false;
    private File activeSerialPort = null;
    private IBinder activeAidlService = null;

    // Common Model 1008 OEM Printer Services
    private static final String[] AIDL_ACTIONS = {
        "woyou.aidlservice.jiuiv5.IWoyouService",
        "com.iposprinter.iposprinterservice.IPosPrinterService",
        "net.nyx.printerservice.IPrinterService",
        "com.ctk.sdk.PosService",
        "com.zcs.pos.service",
        "com.android.print.service"
    };

    private static final String[] SERIAL_PORTS = {
        "/dev/ttyS1",
        "/dev/ttyMT1",
        "/dev/ttyS3",
        "/dev/ttyMT0",
        "/dev/ttyS0",
        "/dev/ttyHS0",
        "/dev/ttyHS1"
    };

    public Model1008PrinterDriver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void connect() {
        // Attempt early connection or binding
        for (String action : AIDL_ACTIONS) {
            Intent intent = new Intent(action);
            intent.setPackage(action.substring(0, action.lastIndexOf('.')));
            try {
                boolean bound = context.bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.i(TAG, "Bound to OEM Printer Service: " + name);
                        activeAidlService = service;
                        isConnected = true;
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.w(TAG, "Disconnected from OEM Printer Service: " + name);
                        activeAidlService = null;
                        isConnected = false;
                    }
                }, Context.BIND_AUTO_CREATE);

                if (bound) {
                    isConnected = true;
                    Log.i(TAG, "Model 1008 AIDL service binding initiated for: " + action);
                    return;
                }
            } catch (Exception ignored) {}
        }

        isConnected = true;
    }

    @Override
    public void disconnect() {
        isConnected = false;
        activeAidlService = null;
    }

    @Override
    public void printReceipt(OrderModels.OrderRecord order, String terminalId, PrintCallback callback) {
        new Thread(() -> {
            try {
                byte[] rawEscPos = EscPosBuilder.buildReceiptBytes(order, terminalId);
                dispatchPrintHeadBytes(rawEscPos);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Physical print failed: " + e.getMessage(), e);
                if (callback != null) callback.onFailure(e.getMessage());
            }
        }).start();
    }

    @Override
    public void feedPaper(int lines) {
        new Thread(() -> {
            try {
                byte[] feed = new byte[]{0x1B, 0x64, (byte) Math.max(1, lines)};
                dispatchPrintHeadBytes(feed);
            } catch (Exception e) {
                Log.e(TAG, "Feed paper failed: " + e.getMessage(), e);
            }
        }).start();
    }

    @Override
    public void testPrint(PrintCallback callback) {
        new Thread(() -> {
            try {
                byte[] testBytes = EscPosBuilder.buildTestPrintBytes("POS-1008");
                dispatchPrintHeadBytes(testBytes);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Test print failed: " + e.getMessage(), e);
                if (callback != null) callback.onFailure(e.getMessage());
            }
        }).start();
    }

    /**
     * Sends ESC/POS data to the physical print head.
     * Throws an IOException if all physical outputs fail, so the UI NEVER claims success falsely!
     */
    @SuppressLint("MissingPermission")
    private synchronized void dispatchPrintHeadBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("No print data generated.");
        }

        StringBuilder failureReasons = new StringBuilder();

        // 1. Direct hardware connection to 'InnerPrinter' via Bluetooth SPP (Verified on Model 1008)
        try {
            if (printViaBluetoothSpp(data)) {
                return;
            }
        } catch (Exception e) {
            failureReasons.append("[Bluetooth InnerPrinter: ").append(e.getMessage()).append("] ");
        }

        // 2. OEM AIDL Service Binder
        try {
            if (printViaAidlService(data)) {
                return;
            }
        } catch (Exception e) {
            failureReasons.append("[AIDL Service: ").append(e.getMessage()).append("] ");
        }

        // 3. Serial Device Port
        try {
            if (printViaSerialDevice(data)) {
                return;
            }
        } catch (Exception e) {
            failureReasons.append("[Serial: ").append(e.getMessage()).append("] ");
        }

        // Physical print failed on all channels!
        throw new IOException("Could not print receipt to InnerPrinter. " + failureReasons.toString());
    }

    @SuppressLint("MissingPermission")
    private boolean printViaBluetoothSpp(byte[] data) throws IOException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IOException("Device does not support Bluetooth.");
        }

        if (!adapter.isEnabled()) {
            throw new IOException("Bluetooth is turned OFF. Please swipe down from top of POS screen and turn ON Bluetooth.");
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            throw new IOException("No paired devices found. Please ensure InnerPrinter is paired under Android Settings -> Connected Devices.");
        }

        BluetoothDevice targetPrinter = null;

        // Exact match for "InnerPrinter" (as shown in device settings)
        for (BluetoothDevice dev : pairedDevices) {
            String name = dev.getName() != null ? dev.getName().trim() : "";
            if ("InnerPrinter".equalsIgnoreCase(name)) {
                targetPrinter = dev;
                break;
            }
        }

        // Fallback: search for inner / printer / pos / 1008
        if (targetPrinter == null) {
            for (BluetoothDevice dev : pairedDevices) {
                String name = dev.getName() != null ? dev.getName().toLowerCase() : "";
                if (name.contains("inner") || name.contains("print") || name.contains("pos") ||
                    name.contains("1008") || name.contains("mtp") || name.contains("rpp") ||
                    name.contains("58") || name.contains("thermal")) {
                    targetPrinter = dev;
                    break;
                }
            }
        }

        // If only 1 paired device exists, use it
        if (targetPrinter == null && pairedDevices.size() == 1) {
            targetPrinter = pairedDevices.iterator().next();
        }

        if (targetPrinter == null) {
            throw new IOException("InnerPrinter not found in paired devices list. Found: " + pairedDevices.size() + " other devices.");
        }

        Log.i(TAG, "Opening physical connection to: " + targetPrinter.getName() + " (" + targetPrinter.getAddress() + ")");

        // Cancel discovery to prevent connection drop/timeout
        adapter.cancelDiscovery();

        BluetoothSocket socket = null;
        IOException lastException = null;

        // Method 1: Standard SPP UUID
        try {
            socket = targetPrinter.createRfcommSocketToServiceRecord(SPP_UUID);
            socket.connect();
        } catch (IOException e1) {
            Log.w(TAG, "SPP UUID connection failed: " + e1.getMessage() + ", trying RFCOMM Channel 1 fallback...");
            lastException = e1;
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {}
            socket = null;
        }

        // Method 2: Direct RFCOMM Channel 1 (Reflection - standard for POS micro-controllers)
        if (socket == null) {
            try {
                Method m = targetPrinter.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                socket = (BluetoothSocket) m.invoke(targetPrinter, 1);
                socket.connect();
            } catch (Exception e2) {
                Log.w(TAG, "RFCOMM Channel 1 connection failed: " + e2.getMessage() + ", trying Insecure RFCOMM Channel 1...");
                lastException = new IOException("Channel 1 reflection failed: " + e2.getMessage(), e2);
                try {
                    if (socket != null) socket.close();
                } catch (Exception ignored) {}
                socket = null;
            }
        }

        // Method 3: Insecure RFCOMM Channel 1 (Bypasses authentication handshakes)
        if (socket == null) {
            try {
                Method m2 = targetPrinter.getClass().getMethod("createInsecureRfcommSocket", new Class[]{int.class});
                socket = (BluetoothSocket) m2.invoke(targetPrinter, 1);
                socket.connect();
            } catch (Exception e3) {
                Log.e(TAG, "Insecure RFCOMM failed: " + e3.getMessage());
                throw new IOException("Cannot connect to InnerPrinter (" + targetPrinter.getAddress() + "). Error: " + (lastException != null ? lastException.getMessage() : e3.getMessage()));
            }
        }

        // Send ESC/POS data to the print head
        try {
            OutputStream os = socket.getOutputStream();
            os.write(data);
            os.flush();

            // Give the hardware motor time to advance the paper roll
            Thread.sleep(300);
            return true;
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Calls OEM AIDL service via reflection if bound
     */
    private boolean printViaAidlService(byte[] data) {
        if (activeAidlService == null) return false;
        try {
            Method[] methods = activeAidlService.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equalsIgnoreCase("sendRAWData") || m.getName().equalsIgnoreCase("sendRawData")) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    if (paramTypes.length == 1 && paramTypes[0] == byte[].class) {
                        m.invoke(activeAidlService, (Object) data);
                        return true;
                    } else if (paramTypes.length == 2 && paramTypes[0] == byte[].class) {
                        m.invoke(activeAidlService, data, null);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "AIDL print invocation failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Tries writing directly to physical Linux serial ports (/dev/ttyS*)
     */
    private boolean printViaSerialDevice(byte[] data) {
        if (activeSerialPort != null) {
            try (FileOutputStream fos = new FileOutputStream(activeSerialPort)) {
                fos.write(data);
                fos.flush();
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Active serial write failed on " + activeSerialPort + ": " + e.getMessage());
            }
        }

        for (String portPath : SERIAL_PORTS) {
            File p = new File(portPath);
            if (p.exists() && p.canWrite()) {
                try (FileOutputStream fos = new FileOutputStream(p)) {
                    fos.write(data);
                    fos.flush();
                    activeSerialPort = p;
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }
}
