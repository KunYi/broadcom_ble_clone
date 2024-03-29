
package com.broadcom.bt.le.api;

import android.util.Log;

import com.broadcom.bt.service.gatt.BluetoothGattID;

import java.util.ArrayList;

public class GattClientContext
{
    static String TAG = "GattClientContext";

    IBleClientCallback mClientCallback = null;
    String mPeerAddress;
    int mConnID;
    public BluetoothGattID mClientAppId;
    public byte mClientIf;
    public ArrayList<GattServiceDataContext> mServiceDataContexts = new ArrayList();

    public GattClientContext(IBleClientCallback callback)
    {
        this.mClientCallback = callback;
    }

    public void setPeerAddress(String sPeerAddress) {
        this.mPeerAddress = sPeerAddress;
    }

    public String getPeerAddress() {
        return this.mPeerAddress;
    }

    public void setConnID(int connID) {
        this.mConnID = connID;
    }

    public int getConnID() {
        return this.mConnID;
    }

    public IBleClientCallback getClientCallback() {
        return this.mClientCallback;
    }

    public GattServiceDataContext getServiceDataContext(BluetoothGattID svcID, boolean fCreate)
    {
        GattServiceDataContext sdContext = null;

        Log.d(TAG, "getServiceDataContext svcID = " + svcID.toString() + ", instid = "
                + svcID.getInstanceID());

        for (int i = 0; i != this.mServiceDataContexts.size(); i++) {
            Log.d(
                    TAG,
                    "getServiceDataContext context "
                            + i
                            + ": "
                            + ((GattServiceDataContext) this.mServiceDataContexts.get(i)).svcUuid
                                    .toString());
            if (svcID.toString().equals(
                    ((GattServiceDataContext) this.mServiceDataContexts.get(i)).svcUuid.toString())) {
                Log.d(TAG, "getServiceDataContext: Found context");
                sdContext = (GattServiceDataContext) this.mServiceDataContexts.get(i);
                break;
            }
        }

        if ((null == sdContext) && (fCreate)) {
            Log.d(TAG, "Creating new context for " + svcID.toString());
            sdContext = new GattServiceDataContext();
            sdContext.svcUuid = svcID;
            this.mServiceDataContexts.add(sdContext);
        }

        return sdContext;
    }

    public class GattServiceDataContext
    {
        public BluetoothGattID svcUuid;
        public IBleCharacteristicDataCallback callback;

        public GattServiceDataContext()
        {
        }

    }

}
