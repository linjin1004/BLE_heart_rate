package com.example.blelibrabry;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final static String TAG = "blelibrary.MainActivity";
	
	private BluetoothAdapter mBluetoothAdapter;
	private final static int REQUEST_ENABLE_BT = 1;
	private BluetoothLeScanner mBluetoothLeScanner;
	
	private boolean serviceBinded = false;
	private boolean mScanning;
    private Handler mHandler = new Handler(); ;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;
    
    // for connecting
    BluetoothDevice mDevice;
    
    TextView mDataField;
    TextView mConnectionField;
    TextView mDeviceField;
    Button mConnectButton;
    Button mDisconnectButton;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mConnectionField = (TextView) findViewById(R.id.connect_status);
		mDataField = (TextView) findViewById(R.id.heart_rate);
		mDeviceField = (TextView) findViewById(R.id.device_name);
		mConnectButton = (Button) findViewById(R.id.connect_button);
		mConnectButton.setOnClickListener(connectButtonClickListner);
		mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
		mDisconnectButton.setOnClickListener(disconnectButtonClickListner);
		//verify that BLE is supported
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		    Log.d(TAG, "BLE is NOT supported");
		    //finish();
		}else{
			Log.i(TAG, "BLE is supported");
			// Initializes Bluetooth adapter.
			BluetoothManager bluetoothManager =
			        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();
			
			// Ensures Bluetooth is available on the device and it is enabled. If not,
			// displays a dialog requesting user permission to enable Bluetooth.
			if (mBluetoothAdapter == null){	 // Checks if Bluetooth is supported on the device.
				Log.d(TAG, "BLE is NOT supported");
				finish();
			}
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
        	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
        	Log.i(TAG, "mBluetoothAdapter.isEnabled()");
        	
        	Log.i(TAG, "scanning BLE");
			mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    Log.i(TAG, "Time out. Stop scanning");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	@Override
    protected void onPause() {
        super.onPause();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);   
    }
	
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	Log.i(TAG, "Found BLE device: " + device.toString());
					Log.i(TAG, "Device BLE name: " + device.getName());

					if(device.getName() != null && device.getName().equals("PAFERS HR-KIT")){
						mDevice = device;						
	                    if(mDevice != null){
	                    	mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                    	Log.i(TAG, "Stop scan devices and start to connect to PAFERS HR-KIT");
	                    	initialBLEService();
	                    	//connectBLE();
	                    }
					}			
                }
            });
        }
    };
    public View.OnClickListener connectButtonClickListner =
            new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mDevice != null){
						Log.i(TAG, "Connect button pressed. Connecting to " + mDevice.getName());
                    	mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    	connectBLE();
                    	
                    }
				}
    };
    public View.OnClickListener disconnectButtonClickListner =
            new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mDevice != null){
						Log.i(TAG, "Connect button pressed. Connecting to " + mDevice.getName());
                    	mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    	disconnectBLE();                    	
                    }
				}
    };
    // everything above is for scanning
    // connecting starts here
    private BluetoothLeService mBluetoothLeService;
    Intent gattServiceIntent;
    
    public void initialBLEService(){
    	if(gattServiceIntent == null || !serviceBinded){
    		gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            serviceBinded = true;
    	}
    } 
    public void connectBLE(){
    	mBluetoothLeService.connect(mDevice.getAddress());
    }
    public void disconnectBLE(){
    	mBluetoothLeService.disconnect();     
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceBinded){
        	unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
        serviceBinded = false;
    } 
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.setListener(mBluetoothLEListener);
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "Unable to initialize Bluetooth");
                finish();
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
    
    private final BluetoothLEListener mBluetoothLEListener = new BluetoothLEListener() {
		@Override
		public void onBluetoothLEConnected() {
			Log.i(TAG, "ACTION_GATT_CONNECTED");
			runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			        mConnectionField.setText(getResources().getString(R.string.connected));
			        mConnectButton.setVisibility(View.GONE);
			        mDisconnectButton.setVisibility(View.VISIBLE);
			    }
			});
		}

		@Override
		public void onBluetoothLEConnecting() {
			mConnectionField.setText(getResources().getString(R.string.connecting));
			mConnectButton.setVisibility(View.GONE);
	        mDisconnectButton.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onBluetoothLEDisconnected() {
			Log.i(TAG, "ACTION_GATT_DISCONNECTED");
			runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
		        	mConnectionField.setText("DISCONNECTED");
		        	mConnectButton.setVisibility(View.VISIBLE);
		        	mDisconnectButton.setVisibility(View.GONE);
		        	mDataField.setText("");
		        	mDeviceField.setText("");
				 }
			});
		}

		@Override
		public void onBluetoothServiceDiscovered(
			HashMap<String, BluetoothGattCharacteristic> characteristicData) {
			Log.i(TAG, characteristicData.toString());
		}

		@Override
		public void onBluetoothDataAvailable(HashMap<String, String> data) {
			final HashMap<String, String> BluetoothLteExtraData = data;
			Log.i(TAG, BluetoothLteExtraData.toString());
			if(BluetoothLteExtraData.containsKey("Heart Rate Measurement")){
				runOnUiThread(new Runnable() {
				     @Override
				     public void run() {
				    	 mDataField.setText(BluetoothLteExtraData.get("Heart Rate Measurement"));
					 }
				});				    	 
    		}
			if(BluetoothLteExtraData.containsKey("Manufacturer Name String")){
    			runOnUiThread(new Runnable() {
				     @Override
				     public void run() {
				    	 mDeviceField.setText(BluetoothLteExtraData.get("Manufacturer Name String"));
				     }
    			});
    		}
		}    	
    };
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
