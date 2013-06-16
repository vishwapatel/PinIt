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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements InfoWindowAdapter {

    private TextView noteTitleTextView;
    private TextView noteBodyTextView;
    private ImageView notePhotoImageView;
    private TextView noteCreatedAtTextView;
    private View view;
    private Context mContext;

    private Note note;

    private HashMap<String, Note> mNoteStore;
    private LruCache<String, Bitmap> mMemoryCache;

    public CustomInfoWindowAdapter(Context ctx, HashMap<String, Note> noteStore, LruCache<String, Bitmap> memoryCache){
        mContext = ctx;
        mNoteStore = noteStore;
        mMemoryCache = memoryCache;
    }

    @Override
    public View getInfoContents(Marker arg0) {
        return null;
    }

    @Override
    public View getInfoWindow(Marker marker){


        note = mNoteStore.get(marker.getId());

        if(!note.getNoteImageThumbnailUrl().isEmpty()) {
            LayoutInflater inflater = (LayoutInflater) mContext.getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.balloon_infowindow, null);

            noteTitleTextView = (TextView) view.findViewById(R.id.balloon_note_title);
            noteBodyTextView = (TextView) view.findViewById(R.id.balloon_note_body);
            notePhotoImageView = (ImageView) view.findViewById(R.id.balloon_note_image);
            notePhotoImageView.setAdjustViewBounds(true);
            noteCreatedAtTextView = (TextView) view.findViewById(R.id.balloon_create_info);
        }
        else {
            LayoutInflater inflater = (LayoutInflater) mContext.getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.balloon_infowindow_alt, null);

            noteTitleTextView = (TextView) view.findViewById(R.id.balloon_note_title_alt);
            noteBodyTextView = (TextView) view.findViewById(R.id.balloon_note_body_alt);
            noteCreatedAtTextView = (TextView) view.findViewById(R.id.balloon_create_info_alt);
        }

        noteTitleTextView.setText(note.getNoteTitle());

        if(!note.getNoteBody().trim().isEmpty()) {
            noteBodyTextView.setVisibility(TextView.VISIBLE);
            noteBodyTextView.setText(note.getNoteBody());
        }
        else {
            noteBodyTextView.setVisibility(TextView.GONE);
        }

        noteCreatedAtTextView.setText(
                "Created by "+note.getNoteCreator()+" on "+ note.getNoteCreatedAt());

        if(!note.getNoteImageThumbnailUrl().trim().isEmpty()) {
            FetchImageTask fetchImageTask = new FetchImageTask();
            try {
                String noteId = note.getNoteId();
                Bitmap result = mMemoryCache.get(noteId);
                if(result != null) {
                    notePhotoImageView.setImageBitmap(result);
                    notePhotoImageView.setVisibility(ImageView.VISIBLE);
                }
                else {
                    result = fetchImageTask.execute(note.getNoteImageThumbnailUrl()).get();
                    notePhotoImageView.setImageBitmap(result);
                    notePhotoImageView.setVisibility(ImageView.VISIBLE);
                    if(noteId != null && result != null) {
                        mMemoryCache.put(noteId, result);
                    }
                }
            } catch (InterruptedException e) {
                return null;
            } catch (ExecutionException e) {
                return null;
            }
        }
        return view;
    }
    
    class FetchImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... arg0) {
            Bitmap notePhoto = null;
            try {
                if(arg0.equals(null) || arg0.equals(""))
                {
                    notePhotoImageView.setVisibility(ImageView.GONE);
                    return null;
                }
                else
                {
                    notePhoto = BitmapFactory.decodeStream((InputStream) new URL(arg0[0]).getContent());
                }
            } catch (MalformedURLException e) {
                return null;
            } catch (IOException e) {
                return null;
            } 
            return notePhoto;
        }   
    }
}
