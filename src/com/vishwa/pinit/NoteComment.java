package com.vishwa.pinit;

import android.os.Parcel;
import android.os.Parcelable;

public class NoteComment implements Parcelable{
    
    private String mCommentCreator;
    private String mCommentText;
    private String mThumbnailUrl;
    private String mCommentCreatedAt;
    private String mCommentCreatedAtFull;

    public NoteComment(String creator, String commentText, String thumbnailUrl, 
            String createdAt, String commentCreatedAtFull) {
        super();
        mCommentCreator = creator;
        mCommentText = commentText;
        mThumbnailUrl = thumbnailUrl;
        mCommentCreatedAt = createdAt;
        mCommentCreatedAtFull = commentCreatedAtFull;
    }
    
    public NoteComment(Parcel parcel) {
        String[] data = new String[5];

        parcel.readStringArray(data);
        mCommentCreator = data[0];
        mCommentText = data[1];
        mThumbnailUrl = data[2];
        mCommentCreatedAt = data[3];
        mCommentCreatedAtFull = data[4];
    }
    
    public static final Parcelable.Creator<NoteComment> CREATOR = new Parcelable.Creator<NoteComment>() {
        public NoteComment createFromParcel(Parcel in) {
            return new NoteComment(in); 
        }

        public NoteComment[] newArray(int size) {
            return new NoteComment[size];
        }
    };

    public String getCommentText() {
        return mCommentText;
    }

    public String getCommentCreator() {
        return mCommentCreator;
    }
    
    public String getCommentCreatedAt() {
        return mCommentCreatedAt;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }
    
    public String getCommentCreatedAtFull() {
        return mCommentCreatedAtFull;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        String[] data = new String[5];

        data[0] = mCommentCreator;
        data[1] = mCommentText;
        data[2] = mThumbnailUrl;
        data[3] = mCommentCreatedAt;
        data[4] = mCommentCreatedAtFull;

        arg0.writeStringArray(data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((mCommentCreatedAtFull == null) ? 0 : mCommentCreatedAtFull
                        .hashCode());
        result = prime * result
                + ((mCommentCreator == null) ? 0 : mCommentCreator.hashCode());
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
        if (!(obj instanceof NoteComment)) {
            return false;
        }
        NoteComment other = (NoteComment) obj;
        if (mCommentCreatedAtFull == null) {
            if (other.mCommentCreatedAtFull != null) {
                return false;
            }
        } else if (!mCommentCreatedAtFull.equals(other.mCommentCreatedAtFull)) {
            return false;
        }
        if (mCommentCreator == null) {
            if (other.mCommentCreator != null) {
                return false;
            }
        } else if (!mCommentCreator.equals(other.mCommentCreator)) {
            return false;
        }
        return true;
    }
   
}