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

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class CreateNoteActivity extends Activity {

    public static final int REQUEST_CODE_PHOTO_SELECT = 101;

    private EditText mNoteTitleField;
    private EditText mNoteBodyField;
    private ImageView mNotePhotoImageView;
    private ImageView mNotePhotoCloseImageView;
    private Button mShareButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;

    private Bitmap mNotePhoto = null;
    private Bitmap mNotePhotoThumbnail = null;

    private double mLatitude;
    private double mLongitude;
    private String mNoteImageThumbnailUrl = new String();

    private ParseObject mNote;
    private ParseFile mNotePhotoObject;
    private ParseFile mNotePhotoThumbnailObject;
    private ParseGeoPoint mGeoPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_create_note);

        mLatitude = Double.parseDouble(getIntent().getStringExtra("geopoint").split(",")[0]);
        mLongitude = Double.parseDouble(getIntent().getStringExtra("geopoint").split(",")[1]);
        mGeoPoint = new ParseGeoPoint(mLatitude, mLongitude);

        mNoteTitleField = (EditText) findViewById(R.id.create_note_title);
        mNoteBodyField = (EditText) findViewById(R.id.create_note_body);
        mNotePhotoImageView = (ImageView) findViewById(R.id.create_note_photo);
        mNotePhotoCloseImageView = (ImageView) findViewById(R.id.create_note_photo_close_button);
        mShareButton = (Button) findViewById(R.id.create_note_confirm_button);
        mCancelButton = (Button) findViewById(R.id.create_note_cancel_button);
        mProgressBar = (ProgressBar) findViewById(R.id.create_note_progressbar);

        mProgressBar.setVisibility(View.INVISIBLE);

        mNotePhotoImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_PHOTO_SELECT);
            }
        });

        mShareButton.setOnClickListener(new ShareButtonOnClickListener());

        mCancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });
    }

    class ShareButtonOnClickListener implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            if(mNoteTitleField.getText().toString().isEmpty()) {
                mNoteTitleField.setError("Note title cannot be empty");
            }
            else {
                mShareButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);

                mNote = new ParseObject("Note");
                mNote.put("title", mNoteTitleField.getText().toString());
                mNote.put("body", mNoteBodyField.getText().toString());
                mNote.put("geopoint", mGeoPoint);
                mNote.put("creator", ParseUser.getCurrentUser().getUsername());

                if(mNotePhoto == null) {
                    mNote.put("hasPhoto", false);
                    uploadNote();
                }
                else {
                    mNote.put("hasPhoto", true);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    mNotePhoto.compress(Bitmap.CompressFormat.JPEG, 80, byteStream);
                    byte[] photoBytes = byteStream.toByteArray();

                    mNotePhotoObject = new ParseFile("notePhoto.jpg", photoBytes);
                    mNotePhotoObject.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                ByteArrayOutputStream thumbnailByteStream = new ByteArrayOutputStream();
                                mNotePhotoThumbnail.compress(Bitmap.CompressFormat.JPEG, 80, thumbnailByteStream);
                                byte[] photoThumbnailBytes = thumbnailByteStream.toByteArray();

                                mNotePhotoThumbnailObject = new ParseFile("notePhotoThumbnail.jpeg", photoThumbnailBytes);
                                mNotePhotoThumbnailObject.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null) {
                                            uploadNote();
                                        }
                                        else {
                                            mShareButton.setEnabled(true);
                                            String error = e.getMessage().substring(0, 1).toUpperCase()+ 
                                                    e.getMessage().substring(1);
                                            PinItUtils.createAlert("Sorry, we couldn't save this photo", 
                                                    error, 
                                                    CreateNoteActivity.this);
                                        }
                                    }
                                });
                            }
                            else {
                                mShareButton.setEnabled(true);
                                String error = e.getMessage().substring(0, 1).toUpperCase()+ 
                                        e.getMessage().substring(1);
                                PinItUtils.createAlert("Sorry, we couldn't save this photo", 
                                        error, 
                                        CreateNoteActivity.this);
                            }
                        }
                    });
                }
            }
        }	   
    }

    private void uploadNote() {
        if(mNotePhotoObject != null && mNotePhotoThumbnailObject != null) {
            mNote.put("notePhoto", mNotePhotoObject);
            mNote.put("notePhotoThumbnail", mNotePhotoThumbnailObject);
            mNoteImageThumbnailUrl = mNotePhotoThumbnailObject.getUrl();
        }
        ParseACL noteAcl = new ParseACL(ParseUser.getCurrentUser());
        noteAcl.setPublicReadAccess(true);
        mNote.setACL(noteAcl);
        mNote.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                mProgressBar.setVisibility(View.INVISIBLE);
                handleNoteUploadResponse(e);
            }
        });
    }

    private void handleNoteUploadResponse(ParseException e) {
        if(e == null) {
            Intent intent = new Intent();
            intent.putExtra("geopoint", mLatitude + "," + mLongitude);
            intent.putExtra("noteTitle", mNoteTitleField.getText().toString());
            intent.putExtra("noteBody", mNoteBodyField.getText().toString());
            intent.putExtra("noteImageThumbnailUrl", mNoteImageThumbnailUrl);
            intent.putExtra("noteId", mNote.getObjectId());
            String date = mNote.getCreatedAt().toString();
            String[] arr = date.split("\\s");
            String createdDate = arr[1] + " " + arr[2] + ", " + arr[5]; 
            intent.putExtra("noteCreatedAt", createdDate);
            intent.putExtra("noteCreatedAtFull", mNote.getCreatedAt().toString());
            CreateNoteActivity.this.setResult(RESULT_OK, intent);
            finish();
        }
        else {
            mShareButton.setEnabled(true);
            String error = e.getMessage().substring(0, 1).toUpperCase()+e.getMessage().substring(1);

            PinItUtils.createAlert("Sorry, we couldn't save this note",
                    error, 
                    CreateNoteActivity.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        super.onActivityResult(requestCode, resultCode, data);

        Uri photoUri;

        switch (requestCode) {
        case REQUEST_CODE_PHOTO_SELECT:
            if (resultCode == Activity.RESULT_OK) {
                if(mNotePhoto != null) {
                    mNotePhoto.recycle();
                    mNotePhoto = null;
                }
                if(mNotePhotoThumbnail != null) {
                    mNotePhotoThumbnail.recycle();
                    mNotePhotoThumbnail = null;
                }
                
                photoUri = data.getData();
                String absolutePath = 
                        PinItUtils.getAbsolutePathFromUri(getApplicationContext(), photoUri);
                mNotePhoto = PinItUtils.decodeSampledBitmapFromFilePath(absolutePath, 700, 700);

                Matrix matrix = 
                        PinItUtils.getRotationMatrixForImage(getApplicationContext(), photoUri);
                mNotePhotoImageView.setAdjustViewBounds(true);

                if(matrix != null && mNotePhoto != null) {
                    mNotePhoto = Bitmap.createBitmap(
                            mNotePhoto, 0, 0, mNotePhoto.getWidth(), mNotePhoto.getHeight(), matrix, true);
                    mNotePhotoThumbnail = Bitmap.createScaledBitmap(
                            mNotePhoto, mNotePhoto.getWidth()/5, mNotePhoto.getHeight()/5, true);

                    mNotePhotoImageView.setImageBitmap(mNotePhoto);

                    mNotePhotoCloseImageView.setVisibility(View.VISIBLE);
                    mNotePhotoCloseImageView.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            recycleAllBitmaps();
                            mNotePhotoImageView.setImageDrawable(
                                    getResources().getDrawable(R.drawable.plus_sign));
                            mNotePhotoCloseImageView.setVisibility(View.INVISIBLE);
                        }
                    }); 
                }
                else {
                    PinItUtils.createAlert("This is embarrassing", "We couldn't load that image please try" +
                    		" another image", CreateNoteActivity.this);
                    recycleAllBitmaps();
                    mNotePhotoImageView.setImageDrawable(
                            getResources().getDrawable(R.drawable.plus_sign));
                    mNotePhotoCloseImageView.setVisibility(View.INVISIBLE);
                }
            }
            break;
        }
    }
    
    public void recycleAllBitmaps() {
        if(mNotePhoto != null) {
            mNotePhoto.recycle();
            mNotePhoto = null;
        }
        if(mNotePhotoThumbnail != null) {
            mNotePhotoThumbnail.recycle();
            mNotePhotoThumbnail = null;
        }
    }

    @Override
    protected void onDestroy() {
        recycleAllBitmaps();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
