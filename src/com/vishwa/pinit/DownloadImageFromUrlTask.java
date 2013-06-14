package com.vishwa.pinit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class DownloadImageFromUrlTask extends AsyncTask<String, Void, Bitmap> {
    private Context mContext;
    private String mUsername;
    
    public DownloadImageFromUrlTask(Context context, String username) {
        mContext = context;
        mUsername = username;
    }
    
    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        Bitmap userPhoto = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            userPhoto = BitmapFactory.decodeStream(in);
            String filename = mUsername + ".png";
            if(userPhoto != null) {
                try {
                    FileOutputStream outputStream = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
                    userPhoto.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                }
                catch (IOException e) {
                    //We can fail silently here because this was simply a cache update, the app is
                    //built to be resilient to cache misses and fetch the data from Parse when that happens
                }
            }
        } catch (Exception e) {
            
        }
        return userPhoto;
    }
}