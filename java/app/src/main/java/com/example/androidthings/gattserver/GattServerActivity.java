/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.gattserver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import android.preference.*;
import android.widget.*;
import org.apache.http.ssl.*;
import android.service.autofill.*;
import android.view.*;

public class GattServerActivity extends Activity {
    private static final String TAG = GattServerActivity.class.getSimpleName();

    /* Local UI */
    private TextView mLocalTimeView;
	private CheckBox chkb1;
	private CheckBox chkb2;
	private CheckBox chkb3;
	private CheckBox chkb4;
	private CheckBox chkb5;
	private CheckBox chkb6;
	private CheckBox chkb7;
	private CheckBox chkb8;
	private CheckBox chkb9;
	private ToggleButton tglb1;
	
    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mLocalTimeView = (TextView) findViewById(R.id.text_time);
		
		chkb1 = (CheckBox) findViewById(R.id.CheckBox1);
		chkb2 = (CheckBox) findViewById(R.id.CheckBox2);
		chkb3 = (CheckBox) findViewById(R.id.CheckBox3);
		chkb4 = (CheckBox) findViewById(R.id.CheckBox4);
		chkb5 = (CheckBox) findViewById(R.id.CheckBox5);
		chkb6 = (CheckBox) findViewById(R.id.CheckBox6);
		chkb7 = (CheckBox) findViewById(R.id.CheckBox7);
		chkb8 = (CheckBox) findViewById(R.id.CheckBox8);
		chkb9 = (CheckBox) findViewById(R.id.CheckBox9);
		tglb1 = (ToggleButton) findViewById(R.id.ToggleButton1);
		

        // Devices with a display should not go to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }
    }
	
	public void toggleLedSwitch(View view) {
		Log.i(TAG, "Switch Value: " + tglb1.isChecked()); // ((Switch) findViewById(R.id.Switch1)).isChecked());
		notifyRegisteredDevices(tglb1.isChecked()); // ((Switch) findViewById(R.id.Switch1)).isChecked());
		// swch1.setThumbTintList();
		// boolean switchState = ((Switch) findViewById(R.id.Switch1)).isChecked());
	}
	
	// public void setLedStateFromGlobalVariable() {
		// notifyRegisteredDevices(SonyRemPro.ledArray);
	// }
	
	static final String ACTION_BUTTON_LIST_UPDATED = "com.example.androidthings.gattserver.ACTION_BUTTON_LIST_RECEIVED";

    @Override
    protected void onStart() {
        super.onStart();
        // Register for system clock events
        IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_BUTTON_LIST_UPDATED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mTimeReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mTimeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     * Listens for system time changes and triggers a notification to
     * Bluetooth subscribers.
     */
    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long now = System.currentTimeMillis();
            //notifyRegisteredDevices(false);
			Log.i(TAG, "Updating UI");
            updateLocalUi(now);
        }
    };

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(SonyRemPro.SONY_ADVERTISER_MANUFACTURER_ID, SonyRemPro.SONY_ADVERTISER_DATA)
//                .addServiceUuid(new ParcelUuid(SonyRemPro.SONY_CAMERA_REMOTE_CONTROL_SERVICE))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(SonyRemPro.createTimeService());

        // Initialize the local UI
        updateLocalUi(System.currentTimeMillis());
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
//    private void notifyRegisteredDevices(long timestamp, byte adjustReason) {
//        if (mRegisteredDevices.isEmpty()) {
//            Log.i(TAG, "No subscribers registered");
//            return;
//        }
//        byte[] exactTime = TimeProfile.getExactTime(timestamp, adjustReason);
//
//        Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
//        for (BluetoothDevice device : mRegisteredDevices) {
//            BluetoothGattCharacteristic timeCharacteristic = mBluetoothGattServer
//                    .getService(TimeProfile.TIME_SERVICE)
//                    .getCharacteristic(TimeProfile.CURRENT_TIME);
//            timeCharacteristic.setValue(exactTime);
//            mBluetoothGattServer.notifyCharacteristicChanged(device, timeCharacteristic, false);
//        }
//    }
	 
	private void notifyRegisteredDevices(boolean ledEnabled) { // long timestamp, byte adjustReason) {
		// mRegisteredDevices.add(device);
        Log.i(TAG, "NotifyRegisterDevices: " + mRegisteredDevices.toString());
		if (mRegisteredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered");
            return;
        }
		
		
//        byte[] exactTime = TimeProfile.getExactTime(timestamp, adjustReason);
//
//        Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
//        for (BluetoothDevice device : mRegisteredDevices) {
//            BluetoothGattCharacteristic timeCharacteristic = mBluetoothGattServer
//				.getService(TimeProfile.TIME_SERVICE)
//				.getCharacteristic(TimeProfile.CURRENT_TIME);
//            timeCharacteristic.setValue(exactTime);
//            mBluetoothGattServer.notifyCharacteristicChanged(device, timeCharacteristic, false);
//         }
//
		boolean[] ledState = SonyRemPro.getLedArray();
		ledState[SonyRemPro.Enum.LEDMap.TOP_RED_LED] = ledEnabled;
		byte[] notifyData = SonyRemPro.setLedArray(ledState);
		
        Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
        for (BluetoothDevice device : mRegisteredDevices) {
            BluetoothGattCharacteristic notifyCharacteristic = mBluetoothGattServer
				.getService(SonyRemPro.SONY_CAMERA_REMOTE_CONTROL_SERVICE)
				.getCharacteristic(SonyRemPro.SONY_REMOTE_NOTIFY_CHARACTERISTIC);
            notifyCharacteristic.setValue(notifyData);
            mBluetoothGattServer.notifyCharacteristicChanged(device, notifyCharacteristic, false);
         }
    }
	private void updateChecksUi() {
		Log.i(TAG, "Updating checkboxes");
		
		chkb1.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.FOCUS_MINUS]);
		chkb2.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.FOCUS_PLUS]);
		chkb3.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.ZOOM_MINUS]);
		chkb4.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.ZOOM_PLUS]);
		chkb5.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.FOCUS]);
		chkb6.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.SHUTTER]);
		chkb7.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.RECORD]);
		chkb8.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.AUTOFOCUS_TOGGLE]);
		chkb9.setChecked(SonyRemPro.buttonArray[SonyRemPro.Enum.ButtonMap.CUSTOM_BUTTON_1]);
	}

    /**
     * Update graphical UI on devices that support it with the current time.
     */
    private void updateLocalUi(long timestamp) {
        Date date = new Date(timestamp);
        String displayDate = DateFormat.getMediumDateFormat(this).format(date)
                + "\n"
                + DateFormat.getTimeFormat(this).format(date);
        mLocalTimeView.setText(displayDate);
		updateChecksUi();
		}
		
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
				mRegisteredDevices.add(device);
				notifyRegisteredDevices(tglb1.isChecked()); //TODO: Add proper notify trigger later. Need to restore state of LED upon reconnection.
			
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value)
		{
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
			// TODO: Implement this method
			if (SonyRemPro.SONY_REMOTE_WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
				Log.i(TAG, "Received Write Request");
				Log.i(TAG, "Received Value: " + Arrays.toString(value));
				//Log.i(TAG, "Address of Value: " + value);
				//Log.i(TAG, "Response Needed: " + responseNeeded);
				//Log.i(TAG, "Request ID: " + requestId);
				//Log.i(TAG, "Request Device: " + device);
				//Log.i(TAG, "Keycode Index: " + button);
				SonyRemPro.resolveButton(value);
				sendBroadcast(new Intent(ACTION_BUTTON_LIST_UPDATED));
				Log.i(TAG, "Array Values: " + Arrays.toString(SonyRemPro.buttonArray));
				
				if (responseNeeded) {
					mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
				}
			}
		}

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();
            if (TimeProfile.CURRENT_TIME.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read CurrentTime");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        TimeProfile.getExactTime(now, TimeProfile.ADJUST_NONE));
            } else if (TimeProfile.LOCAL_TIME_INFO.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read LocalTimeInfo");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        TimeProfile.getLocalTimeInfo(now));
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }
    };
}
