package com.vishwa.pinit;

public enum MapViewMode {
	YOUR_NOTES(0),
	ALL_NOTES(1);
	
	private int code;
	
	private MapViewMode(int code) {
		this.code = code; 
	}
	
	public int getCode() {
		return code;
	}
	
}
