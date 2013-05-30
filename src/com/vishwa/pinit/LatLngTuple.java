package com.vishwa.pinit;

import com.google.android.gms.maps.model.LatLng;

public class LatLngTuple {
	
	private LatLng southwest;
	private LatLng northeast;

	public LatLngTuple(LatLng southwest, LatLng northeast) {
		this.southwest = southwest;
		this.northeast = northeast;
	}
	
	public LatLng getSouthwest() {
		return southwest;
	}
	
	public LatLng getNortheast() {
		return northeast;
	}
}
