package com.vishwa.pinit;

import android.app.Activity;
import android.view.View;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements InfoWindowAdapter {
	
	private String url;
	
	public CustomInfoWindowAdapter(String url) {
		
	}
	
	@Override
	public View getInfoContents(Marker marker) {
		return null;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		
		return null;
	}

}
