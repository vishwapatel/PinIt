package com.vishwa.pinit;

import android.os.Parcel;
import android.os.Parcelable;

public class NoteLike implements Parcelable{
    
    private String mCommentCreator;
    private String mThumbnailUrl;

    public NoteLike(String creator, String thumbnailUrl) {
        super();
        mCommentCreator = creator;
        mThumbnailUrl = thumbnailUrl;
    }
    
    public NoteLike(Parcel parcel) {
        String[] data = new String[2];

        parcel.readStringArray(data);
        mCommentCreator = data[0];
        mThumbnailUrl = data[1];
    }
    
    public static final Parcelable.Creator<NoteLike> CREATOR = new Parcelable.Creator<NoteLike>() {
        public NoteLike createFromParcel(Parcel in) {
            return new NoteLike(in); 
        }

        public NoteLike[] newArray(int size) {
            return new NoteLike[size];
        }
    };

    public String getCommentCreator() {
        return mCommentCreator;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        String[] data = new String[2];

        data[0] = mCommentCreator;
        data[1] = mThumbnailUrl;
        
        arg0.writeStringArray(data);
    }
}