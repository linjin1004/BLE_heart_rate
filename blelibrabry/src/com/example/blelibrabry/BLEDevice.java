package com.example.blelibrabry;

public class BLEDevice {
	
	private int mHeartRate;
	private String mDeviceName;
	
	public BLEDevice(){
		mHeartRate = 0;
		mDeviceName = "";
	}
	public void setHeatRate(int heart_rate){
		mHeartRate = heart_rate;
	}
	public void setmDeviceName(String device_name){
		mDeviceName = device_name;
	}
	public int getHeartRate(){
		return mHeartRate;
	}
	public String getDeviceName(){
		return mDeviceName;
	}
}
