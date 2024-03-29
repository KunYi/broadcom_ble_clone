
package com.broadcom.bt.le.api;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.broadcom.bt.service.gatt.IBluetoothGatt;

/**
 * Provides helper functions and related constants to extend Bluetooth
 * functionality for the Low Energy profile.
 */
public class BleAdapter
{
    private static final String TAG = "BleAdapter";
    private static final boolean D = true;

    // major, api level, minor
    private static final String FRAMEWORK_VERSION = "0.5.0";
    private static final int API_LEVEL = 5;
    private static IBluetoothGatt mService;
    private GattServiceConnection mSvcConn;
    private Context mContext;

    /**
     * Extra for the ACTION_FOUND intent specifying the type of the remote
     * device.
     * 
     * @see {@link #DEVICE_TYPE_BREDR}, {@link #DEVICE_TYPE_BLE},
     *      {@link #DEVICE_TYPE_DUMO}
     */
    public static final String EXTRA_DEVICE_TYPE = "android.bluetooth.device.extra.DEVICE_TYPE";

    /**
     * Identifies a remote Bluetooth device as type BR/EDR, not capable of
     * accepting Bluetooth Low Energy connections.
     */
    public static final byte DEVICE_TYPE_BREDR = 1;

    /**
     * Designates a remote device as a Bluetooth Low Energy (only) device.
     */
    public static final byte DEVICE_TYPE_BLE = 2;

    /**
     * Specifies a remote device is both capable of Bluetooth Low Energy as well
     * as traditional Bluetooth HCI level communication.
     */
    public static final byte DEVICE_TYPE_DUMO = 3;

    /**
     * Broadcast Action: This intent is used to broadcast the UUID wrapped as a
     * android.os.ParcelUuid of the remote device after it has been fetched.
     * This intent is sent only when the UUIDs of the remote device are
     * requested to be fetched using Service Discovery Protocol
     * <ul>
     * <li>Always contains the extra field {@link #EXTRA_DEVICE}</li>
     * <li>Always contains the extra field {@link #EXTRA_UUID}</li>
     * </ul>
     */
    public static final String ACTION_UUID = "android.bluetooth.le.device.action.UUID";

    /**
     * Used as an extra field in {@link #ACTION_UUID} intents, Contains the
     * android.os.ParcelUuid's of the remote device which is a parcelable
     * version of a UUID.
     */
    public static final String EXTRA_UUID = "android.bluetooth.le.device.extra.UUID";

    /**
     * Used as a Parcelable BluetoothDevice extra field in every intent
     * broadcast by this class. It contains the BluetoothDevice that the intent
     * applies to.
     */
    public static final String EXTRA_DEVICE = "android.bluetooth.le.device.extra.DEVICE";

    private static boolean startService() {
        if (mService != null)
            return true;
        IBinder service = ServiceManager.getService(BleConstants.BLUETOOTH_LE_SERVICE);
        if (service != null)
            mService = IBluetoothGatt.Stub.asInterface(service);
        return mService != null;
    }

    /**
     * Constructs a new BleAdapter object
     */
    public BleAdapter(Context ctx) {
        this.mContext = ctx;
        if (startService()==false)
            throw new RuntimeException("failed connecting to service");
        this.init();
    }

    public static boolean checkAPIAvailability() {
        return startService();
    }
    
    public static String getFrameworkVersion()
    {
        return FRAMEWORK_VERSION;
    }

    /**
     * Returns the current Open Bluetooth Low Energy SDK API level.
     * 
     * @return
     */
    public static int getApiLevel()
    {
        return API_LEVEL;
    }

    /**
     * Returns type of the Bluetooth device (LE, BR/EDR or dual-mode).
     * 
     * @param device - The remote device who's type is to be determined
     * @return The type of the remote device
     * @see {@link #DEVICE_TYPE_BREDR}, {@link #DEVICE_TYPE_BLE},
     *      {@link #DEVICE_TYPE_DUMO}
     */
    public static byte getDeviceType(BluetoothDevice device)
    {
        if (!startService())
            throw new RuntimeException("service not available");
        if (device != null) {
            try {
                return mService.getDeviceType(device.getAddress());
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }

        return 0;
    }

    /**
     * Initiates Bluetooth service discovery on a remote device. The results of
     * the service discovery are broadcast using the {@link #ACTION_UUID}
     * intent.
     * 
     * @param deviceAddress - Bluetooth address of the remote device in
     *            00:11:22:33:44:55 format
     * @return true if the device discovery was started successfully
     */
    public static boolean getRemoteServices(String deviceAddress)
    {
        if (!startService())
            throw new RuntimeException("service not available");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null)
            return false;
        try {
            mService.getUUIDs(deviceAddress);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            if (D)
                Log.e(TAG, "error", e);
        }
        return false;
    }

    /**
     * Initializes this BleAdapter object. A connection to the Bluetooth GATT
     * service is made and the onInitialized(boolean) callback is invoked.
     */
    public void init()
    {
        /**
         * TODO: implement
         */
        Log.d("BleAdapter", "init");
        Intent i = new Intent();
        i.setClassName("com.broadcom.bt.app.system",
                "com.broadcom.bt.app.system.GattService");
        mContext.bindService(i, mSvcConn, 1);
    }

    public synchronized void finish()
    {
        if (mSvcConn != null) {
            mContext.unbindService(mSvcConn);
            mSvcConn = null;
        }
    }

    public void finalize() {
        finish();
    }

    /**
     * Defines how aggressive the local devices scans for remote LE devices when
     * a background connection has been requested.
     * 
     * @param scanInterval
     * @param scanWindow
     */
    public void setScanParameters(int scanInterval, int scanWindow)
    {
        try
        {
            mService.setScanParameters(scanInterval, scanWindow);
        } catch (RemoteException e) {
            Log.e("BleAdapter", e.toString());
        }
    }

    void observeStart(int duration)
    {
        try
        {
            if (mService != null)
                mService.observe(true, duration);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling observe: " + e.toString());
        }
    }

    void observeStop()
    {
        try
        {
            if (mService != null)
                mService.observe(false, 0);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling observe: " + e.toString());
        }
    }

    void filterEnable(boolean enable)
    {
        try
        {
            if (mService != null)
                mService.filterEnable(enable);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling filterEnable: " + e.toString());
        }
    }

    void filterEnableBDA(boolean enable, int addr_type, String address)
    {
        try
        {
            if (mService != null)
                mService.filterEnableBDA(enable, addr_type, address);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling filterEnableBDA: " + e.toString());
        }
    }

    void filterManufacturerData(int company, byte[] data1, byte[] data2, byte[] data3,
            byte[] data4)
    {
        try
        {
            if (mService != null)
                mService.filterManufacturerData(company, data1, data2, data3, data4);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling filterManufacturerData: " + e.toString());
        }
    }

    void filterManufacturerDataBDA(int company, byte[] data1, byte[] data2, byte[] data3,
            byte[] data4, boolean has_bda, int addr_type, String address)
    {
        try
        {
            if (mService != null)
                mService.filterManufacturerDataBDA(company, data1, data2, data3, data4,
                        has_bda, addr_type, address);
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling filterManufacturerDataBDA: " + e.toString());
        }
    }

    void clearManufacturerData()
    {
        try
        {
            if (mService != null)
                mService.clearManufacturerData();
        } catch (RemoteException e) {
            Log.d("BleAdapter", "Error calling observe: " + e.toString());
        }
    }

    /**
     * Callback invoked when the BleAdapter has been initialized and has
     * successfully connected to the GATT service.
     * 
     * @param success
     */
    public void onInitialized(boolean success)
    {
    }

    private class GattServiceConnection implements ServiceConnection
    {
        private Context context;

        public GattServiceConnection(Context ctx) {
            context = ctx;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null)
                try {
                    BleAdapter.this.mService =
                            IBluetoothGatt.Stub.asInterface(service);
                    BleAdapter.this.onInitialized(true);
                } catch (Throwable t) {
                    Log.e("BleAdapter", "Unable to get Binder to GattService", t);
                    BleAdapter.this.onInitialized(false);
                }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d("BleAdapter", "Disconnected from GattService!");
        }

    }

}
