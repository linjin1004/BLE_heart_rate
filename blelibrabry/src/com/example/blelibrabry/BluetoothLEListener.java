package com.example.blelibrabry;

import java.util.HashMap;

import android.bluetooth.BluetoothGattCharacteristic;

public interface BluetoothLEListener {
	void onBluetoothLEConnected();
	void onBluetoothLEConnecting();
	void onBluetoothLEDisconnected();
	void onBluetoothServiceDiscovered(HashMap<String, BluetoothGattCharacteristic> characteristicData);
	void onBluetoothDataAvailable(HashMap<String, String> data);
}
