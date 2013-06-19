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

    private String mNoteId;
    private String mNoteTitle;
    private String mNoteBody;
    private String mNoteCreator;
    private String mNoteThumbnailUrl;
    private String mNoteCreatedAt;
    private String mNoteCreatedAtFull;
    private double mNoteLatitude;
    private double mNoteLongitude;
    private boolean mNoteCreatorHasDefaultPhoto;

    public Note(String mNoteId, String mNoteCreator, String mNoteTitle, String mNoteBody, LatLng geopoint, 
            String url, String mNoteCreatedAt, String mNoteCreatedAtFull) {
        this.mNoteId = mNoteId;
        this.mNoteTitle = mNoteTitle;
        this.mNoteBody = mNoteBody;
        this.mNoteCreator = mNoteCreator;
        this.mNoteThumbnailUrl = url;
        this.mNoteCreatedAt = mNoteCreatedAt;
        this.mNoteCreatedAtFull = mNoteCreatedAtFull;
        this.mNoteLatitude = geopoint.latitude;
        this.mNoteLongitude = geopoint.longitude;
        this.mNoteCreatorHasDefaultPhoto = false;
    }

    public Note(Parcel parcel) {
        String[] data = new String[9];

        parcel.readStringArray(data);
        mNoteId = data[0];
        mNoteTitle = data[1];
        mNoteBody = data[2];
        mNoteCreator = data[3];
        mNoteThumbnailUrl = data[4];
        mNoteCreatedAt = data[5];
        mNoteCreatedAtFull = data[6];
        mNoteLatitude = Double.parseDouble(data[7]);
        mNoteLongitude = Double.parseDouble(data[8]);
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
        return mNoteTitle;
    }

    public String getNoteBody() {
        return mNoteBody;
    }

    public String getNoteCreator() {
        return mNoteCreator;
    }

    public String getNoteImageThumbnailUrl() {
        return mNoteThumbnailUrl;
    }

    public double getNoteLatitude() {
        return mNoteLatitude;
    }

    public double getNoteLongitude() {
        return mNoteLongitude;
    }

    public String getNoteCreatedAt() {
        return mNoteCreatedAt;
    }

    public String getNoteCreatedAtFull() {
        return mNoteCreatedAtFull;
    }

    public String getNoteId() {
        return mNoteId;
    }

    public void setNoteCreatorHasDefaultPhoto(boolean b) {
        mNoteCreatorHasDefaultPhoto = b;
    }

    public boolean getNoteCreatorHasDefaultPhoto() {
        return mNoteCreatorHasDefaultPhoto;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(mNoteCreator);
        builder.append(", ");
        builder.append(mNoteTitle);
        builder.append(", ");
        builder.append(mNoteBody);
        builder.append(", ");
        builder.append(mNoteLatitude);
        builder.append(", ");
        builder.append(mNoteLongitude);
        builder.append(", ");
        builder.append(mNoteThumbnailUrl);
        builder.append(", ");
        builder.append(mNoteCreatedAt);
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        String[] data = new String[9];

        data[0] = mNoteId;
        data[1] = mNoteTitle;
        data[2] = mNoteBody;
        data[3] = mNoteCreator;
        data[4] = mNoteThumbnailUrl;
        data[5] = mNoteCreatedAt;
        data[6] = mNoteCreatedAtFull;
        data[7] = Double.toString(mNoteLatitude);
        data[8] = Double.toString(mNoteLongitude);

        arg0.writeStringArray(data);
    }
}
