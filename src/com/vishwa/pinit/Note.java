/*
 * Copyright 2013 Vishwa Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the 'assets' directory of this 
 * application or at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vishwa.pinit;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class Note implements Parcelable{

    private String id;
    private String title;
    private String body;
    private String creator;
    private String thumbnailUrl;
    private String createdAt;
    private String createdAtFull;
    private double latitude;
    private double longitude;
    private boolean creatorHasDefaultPhoto;

    public Note(String id, String creator, String title, String body, LatLng geopoint, 
            String url, String createdAt, String createdAtFull) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.creator = creator;
        this.thumbnailUrl = url;
        this.createdAt = createdAt;
        this.createdAtFull = createdAtFull;
        this.latitude = geopoint.latitude;
        this.longitude = geopoint.longitude;
        this.creatorHasDefaultPhoto = false;
    }

    public Note(Parcel parcel) {
        String[] data = new String[9];

        parcel.readStringArray(data);
        id = data[0];
        title = data[1];
        body = data[2];
        creator = data[3];
        thumbnailUrl = data[4];
        createdAt = data[5];
        createdAtFull = data[6];
        latitude = Double.parseDouble(data[7]);
        longitude = Double.parseDouble(data[8]);
    }

    public static final Parcelable.Creator<Note> CREATOR = new Parcelable.Creator<Note>() {
        public Note createFromParcel(Parcel in) {
            return new Note(in); 
        }

        public Note[] newArray(int size) {
            return new Note[size];
        }
    };

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

    public double getNoteLatitude() {
        return latitude;
    }

    public double getNoteLongitude() {
        return longitude;
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

    public void setNoteCreatorHasDefaultPhoto(boolean b) {
        creatorHasDefaultPhoto = b;
    }

    public boolean getNoteCreatorHasDefaultPhoto() {
        return creatorHasDefaultPhoto;
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
        builder.append(latitude);
        builder.append(", ");
        builder.append(longitude);
        builder.append(", ");
        builder.append(thumbnailUrl);
        builder.append(", ");
        builder.append(createdAt);
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        String[] data = new String[9];

        data[0] = id;
        data[1] = title;
        data[2] = body;
        data[3] = creator;
        data[4] = thumbnailUrl;
        data[5] = createdAt;
        data[6] = createdAtFull;
        data[7] = Double.toString(latitude);
        data[8] = Double.toString(longitude);

        arg0.writeStringArray(data);
    }
}
