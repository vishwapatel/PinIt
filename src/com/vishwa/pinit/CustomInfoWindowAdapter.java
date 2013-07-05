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

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements InfoWindowAdapter {

    private TextView noteTitleTextView;
    private TextView noteBodyTextView;
    private ImageView notePhotoImageView;
    private TextView noteCreatedAtTextView;
    private View view;
    private Context mContext;
    private LruCache<String, Bitmap> mMemoryCache;
    private ImageLoader mImageLoader;

    private Note note;

    private HashMap<String, Note> mNoteStore;

    public CustomInfoWindowAdapter(Context ctx, HashMap<String, Note> noteStore, 
            LruCache<String, Bitmap> memoryCache, ImageLoader imageLoader){
        mContext = ctx;
        mNoteStore = noteStore;
        mMemoryCache = memoryCache;
        mImageLoader = imageLoader;
    }

    @Override
    public View getInfoContents(Marker arg0) {
        return null;
    }

    @Override
    public View getInfoWindow(final Marker marker){

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
            String noteId = note.getNoteId();
            Bitmap result = mMemoryCache.get(noteId);
            if(result != null) {
                notePhotoImageView.setImageBitmap(result);
                notePhotoImageView.setVisibility(ImageView.VISIBLE);
            }
            else {
                ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) notePhotoImageView.getLayoutParams();
                params.height = 100;
                params.width = 100;
                notePhotoImageView.setImageResource(R.drawable.loading);
                notePhotoImageView.setLayoutParams(params);
                
                notePhotoImageView.setVisibility(ImageView.VISIBLE);
                mImageLoader.get(note.getNoteImageThumbnailUrl(), new ImageListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(mContext, "We couldn't load the note image preview currently, " +
                        		"please try loading this note again", Toast.LENGTH_LONG).show();
                    }
                    
                    @Override
                    public void onResponse(ImageContainer response, boolean isImmediate) {
                        ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) notePhotoImageView.getLayoutParams();
                        Bitmap responseBitmap = response.getBitmap();
                        if(responseBitmap != null) {
                            layoutParams.height = LayoutParams.WRAP_CONTENT;
                            layoutParams.width = LayoutParams.WRAP_CONTENT;
                            notePhotoImageView.setLayoutParams(layoutParams);
                            notePhotoImageView.setImageBitmap(responseBitmap);
                            if (marker != null &&
                                    marker.isInfoWindowShown()) {
                                marker.hideInfoWindow();
                                marker.showInfoWindow();
                            }
                        }
                    }
                });
            }
        }
        return view;
    }
}
