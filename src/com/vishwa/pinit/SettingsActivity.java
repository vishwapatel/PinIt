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
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

public class SettingsActivity extends Activity {

    public static final int REQUEST_CODE_PHOTO_SELECT = 101;

    private ImageView mPhotoImageView;
    private Button mProfilePhotoButton;
    private EditText mEmailField;
    private Button mResetPasswordButton;
    private Button mSaveButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private ProgressBar mPhotoProgressBar;

    private boolean mHasChangedProfilePhoto = false;

    private Bitmap mProfilePhoto;
    private Bitmap mProfilePhotoThumbnail;

    private ParseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_settings);

        mPhotoImageView = (ImageView) findViewById(R.id.settings_photo);
        mProfilePhotoButton = (Button) findViewById(R.id.settings_photo_button);
        mEmailField = (EditText) findViewById(R.id.settings_email_field);
        mResetPasswordButton = (Button) findViewById(R.id.settings_reset_password_button);
        mSaveButton = (Button) findViewById(R.id.settings_save_button);
        mCancelButton = (Button) findViewById(R.id.settings_cancel_button);
        mProgressBar = (ProgressBar) findViewById(R.id.settings_progressbar);
        mPhotoProgressBar = (ProgressBar) findViewById(R.id.settings_photo_progressbar);

        mProgressBar.setVisibility(View.INVISIBLE);

        mCurrentUser = ParseUser.getCurrentUser();

        if(mCurrentUser.getBoolean("isDefaultPhoto")) {
            mPhotoProgressBar.setVisibility(View.INVISIBLE);
            mProfilePhoto = BitmapFactory.decodeResource(getResources(), R.drawable.default_image_signup);
            mProfilePhotoThumbnail = ThumbnailUtils.extractThumbnail(mProfilePhoto, 100, 100);
            mPhotoImageView.setImageBitmap(mProfilePhotoThumbnail);
            mPhotoImageView.setVisibility(ImageView.VISIBLE);
        }
        else {
            mPhotoImageView.setVisibility(View.INVISIBLE);
            ParseFile userPhoto = mCurrentUser.getParseFile("profilePhotoThumbnail");
            userPhoto.getDataInBackground(new GetDataCallback() {

                @Override
                public void done(byte[] data, ParseException e) {
                    if(e == null) {
                        mPhotoProgressBar.setVisibility(View.INVISIBLE);
                        mProfilePhotoThumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
                        mPhotoImageView.setAdjustViewBounds(true);
                        mPhotoImageView.setImageBitmap(mProfilePhotoThumbnail);
                        mPhotoImageView.setVisibility(ImageView.VISIBLE);
                    }
                    else {
                        PinItUtils.createAlert("This is embarrassing", "We couldn't load your profile" +
                                "picture, please try this action again", SettingsActivity.this);
                    }
                }
            });
        }

        mProfilePhotoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_PHOTO_SELECT);
            }
        });

        mResetPasswordButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(isEmpty(mEmailField)) {
                    mEmailField.setError("Password reset email can't be blank");
                    return;
                }
                final String email = mEmailField.getText().toString();
                mCurrentUser.setEmail(email);
                mCurrentUser.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if(e == null) {
                            ParseUser.requestPasswordResetInBackground(email, new RequestPasswordResetCallback() {

                                @Override
                                public void done(ParseException e) {
                                    if(e == null) {
                                        Toast.makeText(getApplicationContext(), "An email has been sent to "
                                                +email+" with a link to reset your password. Please check it.", 
                                                Toast.LENGTH_LONG).show();
                                    }
                                    else {
                                        PinItUtils.createAlert(
                                                "Something's gone wrong", e.getMessage(), SettingsActivity.this);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mHasChangedProfilePhoto) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mCurrentUser.put("isDefaultPhoto", false);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    mProfilePhotoThumbnail.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                    byte[] photoBytes = byteStream.toByteArray();

                    CachePhotoTask cachePhotoTask = new CachePhotoTask();
                    cachePhotoTask.doInBackground((Void[]) null);

                    final ParseFile userPhotoThumbnail = 
                            new ParseFile("photoThumbnail.png", photoBytes);
                    userPhotoThumbnail.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                mCurrentUser.put("profilePhotoThumbnail", userPhotoThumbnail);
                                mCurrentUser.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null) {
                                            Toast.makeText(getApplicationContext(), "Profile picture changed " +
                                                    "successfully!", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent();
                                            intent.putExtra("wasPhotoChanged", true);
                                            setResult(RESULT_OK, intent);
                                            finish();
                                        }
                                    }
                                });
                            }
                            else {
                                String error = e.getMessage().substring(0, 1).toUpperCase( )+ 
                                        e.getMessage().substring(1);
                                PinItUtils.createAlert("Sorry, we couldn't save this photo", 
                                        error, SettingsActivity.this);
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(getApplicationContext(), "You haven't changed your profile " +
                            "picture. Please pick a new photo and then press save or else press cancel", 
                            Toast.LENGTH_LONG).show();
                    return;
                }				
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }

        });
    }

    private boolean isEmpty(EditText textField) {
        return textField.getText().toString().trim().isEmpty();
    }

    class CachePhotoTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String filename = mCurrentUser.getUsername() + ".png";

            if(mProfilePhotoThumbnail != null) {
                try {
                    FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    mProfilePhotoThumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                }
                catch (IOException e) {
                    //We can afford to silently fail here because the user photos are loaded from the
                    //Parse back-end if there is a cache miss.
                }
            }
            return null;
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
                mHasChangedProfilePhoto = true;
                photoUri = data.getData();

                try {
                    mProfilePhoto = 
                            MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                    Matrix matrix = 
                            PinItUtils.getRotationMatrixForImage(getApplicationContext(), photoUri);

                    mPhotoImageView.setAdjustViewBounds(true);

                    mProfilePhotoThumbnail = ThumbnailUtils.extractThumbnail(mProfilePhoto, 100, 100);
                    mProfilePhoto.recycle();
                    int profilePhotoThumbnailWidth = mProfilePhotoThumbnail.getWidth();
                    int profilePhotoThumbnailHeight = mProfilePhotoThumbnail.getHeight();
                    mProfilePhotoThumbnail = Bitmap.createBitmap(mProfilePhotoThumbnail, 0, 0, 
                            profilePhotoThumbnailWidth, profilePhotoThumbnailHeight, matrix, true);

                    mPhotoImageView.setImageBitmap(mProfilePhotoThumbnail);
                } catch (IOException e) {
                    PinItUtils.createAlert("This is embarrassing", 
                            "Please try choosing a photo again!", SettingsActivity.this);
                }	
            }
            break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(mProfilePhoto != null) {
            mProfilePhoto.recycle();
            mProfilePhoto = null;
        }
        if(mProfilePhotoThumbnail != null) {
            mProfilePhotoThumbnail.recycle();
            mProfilePhotoThumbnail = null;
        }
        super.onDestroy();
    }


}
