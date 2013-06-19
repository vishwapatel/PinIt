package com.vishwa.pinit;

import android.os.Parcel;
import android.os.Parcelable;

public class NoteComment implements Parcelable{
    
    private String mCommentCreator;
    private String mCommentText;
    private String mThumbnailUrl;
    private String mCommentCreatedAt;

    public NoteComment(String creator, String commentText, String thumbnailUrl, String createdAt) {
        super();
        mCommentCreator = creator;
        mCommentText = commentText;
        mThumbnailUrl = thumbnailUrl;
        mCommentCreatedAt = createdAt;
    }
    
    public NoteComment(Parcel parcel) {
        String[] data = new String[4];

        parcel.readStringArray(data);
        mCommentCreator = data[0];
        mCommentText = data[1];
        mThumbnailUrl = data[2];
        mCommentCreatedAt = data[3];
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
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        String[] data = new String[4];

        data[0] = mCommentCreator;
        data[1] = mCommentText;
        data[2] = mThumbnailUrl;
        data[3] = mCommentCreatedAt;

        arg0.writeStringArray(data);
    }
}