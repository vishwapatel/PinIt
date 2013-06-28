package com.vishwa.pinit;

import android.os.Parcel;
import android.os.Parcelable;

public class NoteLike implements Parcelable{
    
    private String mCommentCreator;
    private String mThumbnailUrl;
    private String mNoteId;

    public NoteLike(String creator, String thumbnailUrl, String noteId) {
        super();
        mCommentCreator = creator;
        mThumbnailUrl = thumbnailUrl;
        mNoteId = noteId;
    }
    
    public NoteLike(Parcel parcel) {
        String[] data = new String[3];

        parcel.readStringArray(data);
        mCommentCreator = data[0];
        mThumbnailUrl = data[1];
        mNoteId = data[2];
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
        String[] data = new String[3];

        data[0] = mCommentCreator;
        data[1] = mThumbnailUrl;
        data[2] = mNoteId;
        
        arg0.writeStringArray(data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mCommentCreator == null) ? 0 : mCommentCreator.hashCode());
        result = prime * result + ((mNoteId == null) ? 0 : mNoteId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NoteLike)) {
            return false;
        }
        NoteLike other = (NoteLike) obj;
        if (mCommentCreator == null) {
            if (other.mCommentCreator != null) {
                return false;
            }
        } else if (!mCommentCreator.equals(other.mCommentCreator)) {
            return false;
        }
        if (mNoteId == null) {
            if (other.mNoteId != null) {
                return false;
            }
        } else if (!mNoteId.equals(other.mNoteId)) {
            return false;
        }
        return true;
    }
    
    
    
}