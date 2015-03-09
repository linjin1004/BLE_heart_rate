package com.example.blelibrabry;

import java.util.HashMap;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class BluetoothLeControlxxx {
	
	private final static String TAG = BluetoothLeControlxxx.class.getSimpleName();
	
	private BluetoothLeService mBluetoothLeService;
    Intent gattServiceIntent;
    private boolean serviceBinded = false;
    HashMap<String, BluetoothGattCharacteristic> characteristicData = new HashMap<String, BluetoothGattCharacteristic>();
    Context mContext;
    BluetoothDevice mDevice;
    
    public BluetoothLeControlxxx(Context context, BluetoothDevice device){
    	mContext = context;
    	mDevice = device;
    	try{
    		context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    	}catch(Exception e){
    		Log.d(TAG, "Exception for register mGattUpdateReceiver: " + e.toString());
    	}
    	if(gattServiceIntent == null || !serviceBinded){
    		gattServiceIntent = new Intent(context, BluetoothLeService.class);
    		mContext.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
            serviceBinded = true;
    	}
    }
    public void pause(){
    	 try{
        	 mContext.unregisterReceiver(mGattUpdateReceiver);
        }catch(Exception e){
        	Log.i(TAG, "mGattUpdateReceiver unregisterReceiver exception.");
        }  
    }
    public void destroy(){
    	//if(serviceBinded){
    	try{
        	mContext.unbindService(mServiceConnection);
        //}
    	}catch(Exception e){
    		Log.d(TAG, "Exception for destroy: " + e.toString());
    	}
        mBluetoothLeService = null;
        serviceBinded = false;
    }
    public void connectBLE(){
    	if(mDevice != null){
    		mBluetoothLeService.connect(mDevice.getAddress());
    	}
    }
    public void disconnectBLE(){
    	if(mDevice != null){
    		mBluetoothLeService.disconnect();
    	}
    }
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "Unable to initialize Bluetooth");
                //finish();
            }
            Log.i(TAG, "onServiceConnected");
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String uuid = null;
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            	Log.i(TAG, "ACTION_GATT_CONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            	Log.i(TAG, "ACTION_GATT_DISCONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            	characteristicData = new HashMap<String, BluetoothGattCharacteristic>();
            	if(mBluetoothLeService.getSupportedGattServices() != null){
            		List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
	            	for (BluetoothGattService gattService : gattServices) {
	                    uuid = gattService.getUuid().toString();
	                    
	                    Log.i(TAG, SampleGattAttributes.lookup(uuid, mContext.getResources().getString(R.string.unknown_service)));
	                    Log.i(TAG, "UUID: " + uuid);
	                    
	                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();						
	                    // Loops through available Characteristics.
	                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
	                        uuid = gattCharacteristic.getUuid().toString();
	                        String charac_name = SampleGattAttributes.lookup(uuid, mContext.getResources().getString(R.string.unknown_characteristic));
	                        characteristicData.put(charac_name, gattCharacteristic);
	                        Log.i(TAG, "BluetoothGattCharacteristic: " + charac_name);
	                    }
	                }
	            	Log.i(TAG, characteristicData.toString());
	            	if(characteristicData.containsKey("Manufacturer Name String")){
	            		Log.i(TAG, "characteristicData uuid: " + characteristicData.get("Manufacturer Name String").getUuid());
	            		mBluetoothLeService.getCharacteristcData(characteristicData.get("Manufacturer Name String"));
	            	}
            	}
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	Log.i(TAG, "ACTION_DATA_AVAILABLE");
            	String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            	String chara_name = intent.getStringExtra(BluetoothLeService.CHARACTERISTIC_NAME);
            	if(data != null){
            		Log.i(TAG, "DATA: " + data);
            		if(chara_name.equals("Heart Rate Measurement")){
            			Log.i(TAG, "Heart Rate Measurement: " + data);
            		}else if(chara_name.equals("Manufacturer Name String")){
            			Log.i(TAG, "Manufacturer Name String: " + data);
            			
                		if(characteristicData.containsKey("Heart Rate Measurement")){
                			mBluetoothLeService.getCharacteristcData(characteristicData.get("Heart Rate Measurement"));
    	            	}
            		}
            		Log.i(TAG, "characteristicData:" + characteristicData.toString());
            	}
            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
