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

import android.util.Log;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;


import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import android.bluetooth.le.AdvertiseData;
import android.location.*;

/**
 * Implementation of the Bluetooth GATT Time Profile.
 * https://www.bluetooth.com/specifications/adopted-specifications
 */

public class SonyRemPro
{
	public static boolean[] buttonArray = new boolean[9];
	public static boolean[] ledArray = new boolean[1];

	public static byte[] setLedArray(boolean[] ledArray)
	{
		if (ledArray[Enum.LEDMap.TOP_RED_LED] == true) {
			return NotifyList.AUTOFOCUS_LOCKED_LED_ON;
		}
		else if (ledArray[Enum.LEDMap.TOP_RED_LED] == false) {
			return NotifyList.AUTOFOCUS_NOT_LOCKED_LED_OFF;
		}
		
		SonyRemPro.ledArray = ledArray;
		return null; //Error condition
	}

	public static boolean[] getLedArray()
	{
		return ledArray;
	}
	
	public static class Enum {
		public static class ButtonMap {
			public static final int FOCUS_MINUS = 0;
			public static final int FOCUS_PLUS = 1;
			public static final int ZOOM_MINUS = 2;
			public static final int ZOOM_PLUS = 3;
			public static final int FOCUS = 4;
			public static final int SHUTTER = 5;
			public static final int RECORD= 6;
			public static final int AUTOFOCUS_TOGGLE = 7;
			public static final int CUSTOM_BUTTON_1 = 8;
		}
	    public static class LEDMap {
			public static final int TOP_RED_LED = 0;
		}
	}
    private static final String TAG = TimeProfile.class.getSimpleName();

//    public static UUID BATTERY_SERVICE = "180f";
    public static UUID SONY_CAMERA_REMOTE_CONTROL_SERVICE = UUID.fromString("8000ff00-ff00-ffff-ffff-ffffffffffff");
    public static UUID SONY_REMOTE_WRITE_CHARACTERISTIC = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    // 0x0FF1
    public static UUID SONY_REMOTE_NOTIFY_CHARACTERISTIC = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    // 0x0FF2
    public static UUID SONY_REMOTE_NOTIFY_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //"2902");

    public static int SONY_ADVERTISER_MANUFACTURER_ID = 0x012D;

    // static List<Byte> adData =  {0x03, 0x00, '0xFF', 0xFF, 0xFF, 0xFF, 0x22, 0xEF, 0x00, 0x21, 0x60, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] SONY_ADVERTISER_DATA = new byte[]  {0x03, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x22, (byte) 0xEF, 0x00, 0x21, 0x60, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

//    public static UUID TEST_AD_DATA_UUID = UUID.fromString("0300FFFF-FFFF-22EF-0021-600000000000"); // 00000000");

//    public static byte[] SONY_ADVERTISER_DATA = getIdAsByte(TEST_AD_DATA_UUID);

    //public static UUID NOTICE_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Current Remote Service.
     */
    public static BluetoothGattService createTimeService() {
        BluetoothGattService service = new BluetoothGattService(SONY_CAMERA_REMOTE_CONTROL_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Remote Command Receiver Characteristic
        BluetoothGattCharacteristic remoteCommand = new BluetoothGattCharacteristic(SONY_REMOTE_WRITE_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        // Remote Notification Characteristic
        BluetoothGattCharacteristic remoteNotify = new BluetoothGattCharacteristic(SONY_REMOTE_NOTIFY_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        // Descriptor for Notify
        BluetoothGattDescriptor notifyDescriptor = new BluetoothGattDescriptor(SONY_REMOTE_NOTIFY_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        remoteNotify.addDescriptor(notifyDescriptor);

        // Local Time Information characteristic
//        BluetoothGattCharacteristic localTime = new BluetoothGattCharacteristic(LOCAL_TIME_INFO,
//                //Read-only characteristic
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ);
//                BluetoothGattCharacteristic.PERMISSION_READ);

        service.addCharacteristic(remoteCommand);
        service.addCharacteristic(remoteNotify);

        return service;
    }
	
	public static void resolveButton(byte[] inputValue) {
		// Log.i(TAG, "Resolve which button is pressed");
		
		/**
		* 0x01 and 0x02 could be length of payload.
		* 	,TODO: Potential optimization to ignore 
		* 	first byte to speed up aquisiton of pressed key.
		* Possible optimization. Use void and use numbers 
		* 	to index into array of pressed and unpressed keys.
		* Additional optimization, split 2nd byte into
		*  upper and lower 16 bits and compare against those.
		**/
		
		switch (inputValue[1]) {
			case ButtonsList.CAMERA_FOCUS_MINUS_KEYDOWN:
				Log.i(TAG, "Camera Focus Minus pressed");
				// buttonArray.set(0, true);
				buttonArray[0] = true;
				break;
				// return 1;
			case ButtonsList.CAMERA_FOCUS_MINUS_KEYUP:
				Log.i(TAG,"Camera Focus Minus released");
				// buttonArray.set(0, false);
				buttonArray[0] = false;
				break;
				// return 2;
			case ButtonsList.CAMERA_FOCUS_PLUS_KEYDOWN:
				Log.i(TAG,"Camera Focus Plus pressed");
				// buttonArray.set(1, true);
				buttonArray[1] = true;
				break;
				// return 3;
			case ButtonsList.CAMERA_FOCUS_PLUS_KEYUP:
				Log.i(TAG,"Camera Focus Plus released");
				// buttonArray.set(1, false);
				buttonArray[1] = false;
				break;
				// return 4;
			case ButtonsList.CAMERA_ZOOM_MINUS_KEYDOWN:
				Log.i(TAG,"Camera Zoom Minus pressed");
				// buttonArray.set(2, true);
				buttonArray[2] = true;
				break;
				// return 5;
			case ButtonsList.CAMERA_ZOOM_MINUS_KEYUP:
				Log.i(TAG,"Camera Zoom Minus released");
				// buttonArray.set(2, false);
				buttonArray[2] = false;
				break;
				// return 6;
			case ButtonsList.CAMERA_ZOOM_PLUS_KEYDOWN:
				Log.i(TAG,"Camera Zoom Plus pressed");
				// buttonArray.set(3, true);
				buttonArray[3] = true;
				break;
				// return 7;
			case ButtonsList.CAMERA_ZOOM_PLUS_KEYUP:
				Log.i(TAG,"Camera Zoom Plus released");
				// buttonArray.set(3, false);
				buttonArray[3] = false;
				break;
				// return 8;
			case ButtonsList.CAMERA_FOCUS_KEYDOWN:
				Log.i(TAG,"Camera Focus Pressed");
				// buttonArray.set(4, true);
				buttonArray[4] = true;
				break;
				// return 9;
			case ButtonsList.CAMERA_FOCUS_KEYUP:
				Log.i(TAG,"Camera Focus released");
				// buttonArray.set(4, false);
				buttonArray[4] = false;
				break;
				// return 10;
			case ButtonsList.CAMERA_SHUTTER_KEYDOWN:
				Log.i(TAG,"Camera Shutter pressed");
				// buttonArray.set(5, true);
				buttonArray[5] = true;
				break;
				// return 11;
			case ButtonsList.CAMERA_SHUTTER_KEYUP:
				Log.i(TAG,"Camera Shutter released");
				// buttonArray.set(5, false);
				buttonArray[5] = false;
				break;
				// return 12;
			case ButtonsList.CAMERA_RECORD_KEYDOWN:
				Log.i(TAG,"Camera Record pressed");
				// buttonArray.set(6, true);
				buttonArray[6] = true;
				break;
				// return 13;
			case ButtonsList.CAMERA_RECORD_KEYUP:
				Log.i(TAG,"Camera Record released");
				// buttonArray.set(6, false);
				buttonArray[6] = false;
				break;
				// return 14;
			case ButtonsList.CAMERA_AUTOFOCUS_ON_KEYDOWN:
				Log.i(TAG,"Camera Autofocus Toggle pressed");
				// buttonArray.set(7, true);
				buttonArray[7] = true;
				break;
				// return 15;
			case ButtonsList.CAMERA_AUTOFOCUS_ON_KEYUP:
				Log.i(TAG,"Camera Autofocus Toggle released");
				// buttonArray.set(7, false);
				buttonArray[7] = false;
				break;
				// return 16;
			case ButtonsList.CAMERA_CUSTOM_1_KEYDOWN:
				Log.i(TAG,"Camera Custom 1 pressed");
				// buttonArray.set(8, true);
				buttonArray[8] = true;
				break;
				// return 17;
			case ButtonsList.CAMERA_CUSTOM_1_KEYUP:
				Log.i(TAG,"Camera Custom 1 released");
				// buttonArray.set(8, false);
				buttonArray[8] = false;
				break;
				// return 18;
			default:
			    Log.i(TAG, "Unknown Keycode");
		}
	}

//public static void buttonArray(int buttonIndex, boolean buttonPressed) 
    //public static ArrayList<Boolean> buttonArray = new ArrayList<Boolean>(9);
		//buttons.set(buttonIndex, buttonPressed);
	// }

    public static byte[] getIdAsByte(UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
//    protected void setAdvertiseData() {
//        AdvertiseData.Builder mBuilder = new AdvertiseData.Builder();
//        ByteBuffer mManufacturerData = ByteBuffer.allocate(24);
//        byte[] uuid = getIdAsByte(UUID.fromString("0CF052C297CA407C84F8B62AAC4E9020"));
//        mManufacturerData.put(0, (byte) 0xBE); // Beacon Identifier
//        mManufacturerData.put(1, (byte) 0xAC); // Beacon Identifier
//        for (int i = 2; i <= 17; i++) {
//            mManufacturerData.put(i, uuid[i - 2]); // adding the UUID
//        }
//        mManufacturerData.put(18, (byte) 0x00); // first byte of Major
//        mManufacturerData.put(19, (byte) 0x09); // second byte of Major
//        mManufacturerData.put(20, (byte) 0x00); // first minor
//        mManufacturerData.put(21, (byte) 0x06); // second minor
//        mManufacturerData.put(22, (byte) 0xB5); // txPower
//        mBuilder.addManufacturerData(224, mManufacturerData.array()); // using google's company ID
//        mAdvertiseData = mBuilder.build();
//    }

    /**
     * Construct the field values for a Local Time Information characteristic
     * from the given epoch timestamp.
     */

    /**
     * Convert a raw DST offset (in 30 minute intervals) to the
     * corresponding Bluetooth DST offset code.
     */
	
    public static final class ButtonsList {
//        public static final byte[] CAMERA_FOCUS_KEYDOWN = new byte[] {0x01, 0x07};
//        public static final byte[] CAMERA_FOCUS_KEYUP = new byte[] {0x01, 0x06};
//
//        public static byte[] CAMERA_SHUTTER_KEYDOWN = new byte[] {0x01, 0x09};
//        public static byte[] CAMERA_SHUTTER_KEYUP = new byte[] {0x01, 0x08};
//
//        public static byte[] CAMERA_CAMCORDER_KEYDOWN = new byte[] {0x01, 0x0F};
//        public static byte[] CAMERA_CAMCORDER_KEYUP = new byte[] {0x01, 0x0E};
//
//        public static byte[] CAMERA_ZOOM_MINUS_KEYDOWN = new byte[] {0x02, 0x47, 0x20};
//        public static byte[] CAMERA_ZOOM_MINUS_KEYUP = new byte[] {0x02, 0x46, 0x00};
//
//        public static byte[] CAMERA_ZOOM_PLUS_KEYDOWN = new byte[] {0x02, 0x45, 0x20};
//        public static byte[] CAMERA_ZOOM_PLUS_KEYUP = new byte[] {0x02, 0x44, 0x00};
//
//        public static final byte[] CAMERA_FOCUS_MINUS_KEYDOWN = new byte[] {0x02, 0x6B, 0x20};
//        public static byte[] CAMERA_FOCUS_MINUS_KEYUP = new byte[] {0x02, 0x6A, 0x00};
//
//        public static byte[] CAMERA_FOCUS_PLUS_KEYDOWN = new byte[] {0x02, 0x6D, 0x20};
//        public static byte[] CAMERA_FOCUS_PLUS_KEYUP = new byte[] {0x02, 0x6C, 0x00};
//
//        public static final byte[] CAMERA_AUTOFOCUS_ON_KEYDOWN = new byte[] {0x01, 0x15};
//        public static final byte[] CAMERA_AUTOFOCUS_ON_KEYUP = new byte[] {0x01, 0x14};
//
//        public static byte[] CAMERA_CUSTOM_1_KEYDOWN = new byte[] {0x01, 0x21};
//        public static byte[] CAMERA_CUSTOM_1_KEYUP = new byte[] {0x01, 0x20};

		
	    public static final byte CAMERA_FOCUS_KEYDOWN = 0x07;
        public static final byte CAMERA_FOCUS_KEYUP = 0x06;

        public static final byte CAMERA_SHUTTER_KEYDOWN = 0x09;
        public static final byte CAMERA_SHUTTER_KEYUP = 0x08;

        public static final byte CAMERA_RECORD_KEYDOWN = 0x0F;
        public static final byte CAMERA_RECORD_KEYUP = 0x0E;

        public static final byte CAMERA_ZOOM_MINUS_KEYDOWN = 0x47;
        public static final byte CAMERA_ZOOM_MINUS_KEYUP = 0x46;

        public static final byte CAMERA_ZOOM_PLUS_KEYDOWN = 0x45;
        public static final byte CAMERA_ZOOM_PLUS_KEYUP = 0x44;

        public static final byte CAMERA_FOCUS_MINUS_KEYDOWN = 0x6B;
        public static final byte CAMERA_FOCUS_MINUS_KEYUP = 0x6A;

        public static final byte CAMERA_FOCUS_PLUS_KEYDOWN = 0x6D;
        public static final byte CAMERA_FOCUS_PLUS_KEYUP = 0x6C;

        public static final byte CAMERA_AUTOFOCUS_ON_KEYDOWN = 0x15;
        public static final byte CAMERA_AUTOFOCUS_ON_KEYUP = 0x14;

        public static final byte CAMERA_CUSTOM_1_KEYDOWN = 0x21;
        public static final byte CAMERA_CUSTOM_1_KEYUP = 0x20;
		// 0x01 and 0x02 could be length of payload.
    }
	public static class NotifyList {
		public static byte[] AUTOFOCUS_LOCKED_LED_ON = new byte[] {0x02, 0x3F, 0x20};
		// Times out at 0 to 11 seconds when remote disconnects. If remote is active (Button held down.)
		//  remote will stay active and lit up.
		// Cannot be extended with a second notify packet.
		// I'm still trying to figure out how the record functionality works.
		//  Maybe it just toggles it off and on really fast before the remote times out?
		//  Or maybe there's an entirely different code to keep the LED on?
		public static byte[] AUTOFOCUS_NOT_LOCKED_LED_OFF = new byte[] {0x02, 0x3F, 0x40};
	}
}

