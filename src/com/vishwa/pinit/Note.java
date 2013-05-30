package com.vishwa.pinit;

import com.google.android.gms.maps.model.Marker;
import com.parse.ParseGeoPoint;

public class Note {
	
	private String id;
	private String title;
	private String body;
	private String creator;
	private String thumbnailUrl;
	private String createdAt;
	private String createdAtFull;
	private ParseGeoPoint geopoint;
	
	public Note(String id, String creator, String title, String body, ParseGeoPoint geopoint, String url, String createdAt, String createdAtFull) {
		this.id = id;
		this.title = title;
		this.body = body;
		this.creator = creator;
		this.geopoint = geopoint;
		this.thumbnailUrl = url;
		this.createdAt = createdAt;
		this.createdAtFull = createdAtFull;
	}
	
	public String getNoteTitle() {
		return title;
	}
	
	public String getNoteBody() {
		return body;
	}
	
	public String getNoteCreator() {
		return creator;
	}
	
	public String getNoteImageThumbnailUrl() {
		return thumbnailUrl;
	}
	
	public ParseGeoPoint getNoteGeoPoint() {
		return geopoint;
	}
	
	public String getNoteCreatedAt() {
		return createdAt;
	}
	
	public String getNoteCreatedAtFull() {
		return createdAtFull;
	}
	
	public String getNoteId() {
		return id;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(creator);
		builder.append(", ");
		builder.append(title);
		builder.append(", ");
		builder.append(body);
		builder.append(", ");
		builder.append(geopoint.toString());
		builder.append(", ");
		builder.append(thumbnailUrl);
		builder.append(", ");
		builder.append(createdAt);
		return builder.toString();
	}
}
