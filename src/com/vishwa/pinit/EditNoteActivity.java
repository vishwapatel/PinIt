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
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

public class EditNoteActivity extends Activity {

    public static final int REQUEST_CODE_PHOTO_SELECT = 101;

    private EditText mNoteTitleField;
    private EditText mNoteBodyField;
    private RadioGroup mPrivacyRadioGroup;
    private ImageView mNotePhotoImageView;
    private ImageView mNotePhotoCloseImageView;
    private Button mSaveButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private ProgressBar mPhotoProgressBar;
    private TextView mNoteTitleCharCount;

    private Bitmap mCurrentPhoto = null;
    private Bitmap mNotePhoto = null;
    private Bitmap mNotePhotoThumbnail = null;

    private String mNoteImageThumbnailUrl = new String();
    private Note mEditNote;
    private boolean mHasPhoto = false;
    private boolean mHasChangedPhoto = false;
    private int mNoteTitleLengthLimit = 80;

    private ParseFile mNotePhotoObject;
    private ParseFile mNotePhotoThumbnailObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_note);

        mEditNote = (Note) getIntent().getExtras().getParcelable("note");

        mNoteTitleField = (EditText) findViewById(R.id.edit_note_title);
        mNoteBodyField = (EditText) findViewById(R.id.edit_note_body);
        mPrivacyRadioGroup = (RadioGroup) findViewById(R.id.edit_note_radio_group);
        mNotePhotoImageView = (ImageView) findViewById(R.id.edit_note_photo);
        mNotePhotoCloseImageView = (ImageView) findViewById(R.id.edit_note_photo_close_button);
        mSaveButton = (Button) findViewById(R.id.edit_note_confirm_button);
        mCancelButton = (Button) findViewById(R.id.edit_note_cancel_button);
        mProgressBar = (ProgressBar) findViewById(R.id.edit_note_progressbar);
        mPhotoProgressBar = (ProgressBar) findViewById(R.id.edit_note_photo_progressbar);
        mNoteTitleCharCount = (TextView) findViewById(R.id.edit_note_title_character_count);

        mProgressBar.setVisibility(View.INVISIBLE);
        mPhotoProgressBar.setVisibility(View.INVISIBLE);

        mNoteTitleField.setText(mEditNote.getNoteTitle());
        mNoteBodyField.setText(mEditNote.getNoteBody());
        mNoteTitleCharCount.setText(
                Integer.toString(mNoteTitleLengthLimit - mNoteTitleField.getText().length()));
        
        mNoteTitleField.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                int count = mNoteTitleLengthLimit - mNoteTitleField.getText().length();
                mNoteTitleCharCount.setText(Integer.toString(count));
                if(count < 10) {
                    mNoteTitleCharCount.setTextColor(Color.RED);
                }
                else {
                    mNoteTitleCharCount.setTextColor(Color.rgb(117, 117, 117));
                }
            }
        });

        if(!mEditNote.getNoteImageThumbnailUrl().isEmpty()) {
            mHasPhoto = true;
            mNotePhotoImageView.setVisibility(View.INVISIBLE);
            mPhotoProgressBar.setVisibility(View.VISIBLE);
            ParseQuery query = new ParseQuery("Note");
            query.getInBackground(mEditNote.getNoteId(), new GetCallback() {

                @Override
                public void done(ParseObject note, ParseException e) {
                    if(e == null) {
                        ParseFile notePhotoFile = note.getParseFile("notePhoto");
                        notePhotoFile.getDataInBackground(new GetDataCallback() {

                            @Override
                            public void done(byte[] data, ParseException e) {
                                if(e == null) {
                                    mPhotoProgressBar.setVisibility(ProgressBar.GONE);
                                    mNotePhotoImageView.setVisibility(ImageView.VISIBLE);
                                    mNotePhotoImageView.setAdjustViewBounds(true);
                                    mCurrentPhoto = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    mNotePhotoImageView.setImageBitmap(mCurrentPhoto);
                                }
                                else {
                                    PinItUtils.createAlert("This is embarrassing", 
                                            "We couldn't load the note photo, please try editing " +
                                                    "this note again", EditNoteActivity.this);
                                }
                            }
                        });
                    }
                    else {
                        PinItUtils.createAlert("This is embarrassing", 
                                "We couldn't load the note photo, please try editing this note again", 
                                EditNoteActivity.this);
                    }
                }
            });
        }

        mNotePhotoImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_PHOTO_SELECT);
            }
        });

        mSaveButton.setOnClickListener(new ShareButtonOnClickListener());

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
                mSaveButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);

                ParseQuery query = new ParseQuery("Note");
                query.getInBackground(mEditNote.getNoteId(), new GetCallback() {
                    public void done(final ParseObject note, ParseException e) {
                        if (e == null) {
                            
                            int selectedId = mPrivacyRadioGroup.getCheckedRadioButtonId();
                            RadioButton selectedRadioButton = (RadioButton) findViewById(selectedId);
                            
                            note.put("visibility", selectedRadioButton.getText().toString().toLowerCase());
                            
                            if(!mEditNote.getNoteTitle().equals(mNoteTitleField.getText().toString())) {
                                note.put("title", mNoteTitleField.getText().toString());
                            }
                            if(!mEditNote.getNoteBody().equals(mNoteBodyField.getText().toString())) {
                                note.put("body", mNoteBodyField.getText().toString());
                            }
                            if(!mHasChangedPhoto) {
                                note.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null) {
                                            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                                            Toast.makeText(getApplicationContext(), 
                                                    "Note edited successfully!", Toast.LENGTH_SHORT).show();
                                            Note updatedNote = new Note(mEditNote.getNoteId(), 
                                                    mEditNote.getNoteCreator(), 
                                                    mNoteTitleField.getText().toString(), 
                                                    mNoteBodyField.getText().toString(), 
                                                    new LatLng(mEditNote.getNoteLatitude(), 
                                                            mEditNote.getNoteLongitude()), 
                                                            mEditNote.getNoteImageThumbnailUrl(), 
                                                            mEditNote.getNoteCreatedAt(), 
                                                            mEditNote.getNoteCreatedAtFull());
                                            Intent intent = new Intent();
                                            intent.putExtra("note", updatedNote);
                                            intent.putExtra("wasPhotoChanged", mHasChangedPhoto);
                                            setResult(RESULT_OK, intent);
                                            finish();
                                        }
                                        else {
                                            mSaveButton.setEnabled(true);
                                            PinItUtils.createAlert("This is embarrassing", 
                                                    "We couldn't update that note, please try again",
                                                    EditNoteActivity.this);
                                        }
                                    }
                                });
                            }
                            else {
                                note.put("hasPhoto", true);
                                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                                mNotePhoto.compress(Bitmap.CompressFormat.JPEG, 80, byteStream);
                                byte[] photoBytes = byteStream.toByteArray();
                                mNotePhoto.recycle();

                                mNotePhotoObject = new ParseFile("notePhoto.jpg", photoBytes);
                                mNotePhotoObject.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null) {
                                            ByteArrayOutputStream thumbnailByteStream = 
                                                    new ByteArrayOutputStream();
                                            mNotePhotoThumbnail.compress(
                                                    Bitmap.CompressFormat.JPEG, 80, thumbnailByteStream);
                                            byte[] photoThumbnailBytes = thumbnailByteStream.toByteArray();

                                            mNotePhotoThumbnailObject = new ParseFile(
                                                    "notePhotoThumbnail.jpeg", photoThumbnailBytes);
                                            mNotePhotoThumbnailObject.saveInBackground(new SaveCallback() {

                                                @Override
                                                public void done(ParseException e) {
                                                    if(e == null) {
                                                        note.put("notePhoto", mNotePhotoObject);
                                                        note.put("notePhotoThumbnail", mNotePhotoThumbnailObject);
                                                        mNoteImageThumbnailUrl = mNotePhotoThumbnailObject.getUrl();
                                                        note.saveInBackground(new SaveCallback() {

                                                            @Override
                                                            public void done(ParseException e) {
                                                                if(e == null) {
                                                                    mProgressBar.setVisibility(View.INVISIBLE);
                                                                    Toast.makeText(getApplicationContext(), 
                                                                            "Note edited successfully!", Toast.LENGTH_SHORT).show();
                                                                    Note updatedNote = new Note(mEditNote.getNoteId(), 
                                                                            mEditNote.getNoteCreator(), 
                                                                            mNoteTitleField.getText().toString(), 
                                                                            mNoteBodyField.getText().toString(), 
                                                                            new LatLng(mEditNote.getNoteLatitude(), 
                                                                                    mEditNote.getNoteLongitude()), 
                                                                                    mNoteImageThumbnailUrl, 
                                                                                    mEditNote.getNoteCreatedAt(), 
                                                                                    mEditNote.getNoteCreatedAtFull());
                                                                    Intent intent = new Intent();
                                                                    intent.putExtra("note", updatedNote);
                                                                    intent.putExtra("wasPhotoChanged", mHasChangedPhoto);
                                                                    setResult(RESULT_OK, intent);
                                                                    finish();
                                                                }
                                                                else {
                                                                    mSaveButton.setEnabled(true);
                                                                    PinItUtils.createAlert("This is embarrassing", 
                                                                            "We couldn't update that note, please try again",
                                                                            EditNoteActivity.this);
                                                                }
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        mSaveButton.setEnabled(true);
                                                        PinItUtils.createAlert(
                                                                "Sorry, we couldn't edit this note with that photo", 
                                                                "Please try again", 
                                                                EditNoteActivity.this);
                                                    }
                                                }
                                            });
                                        }
                                        else {
                                            mSaveButton.setEnabled(true);
                                            PinItUtils.createAlert(
                                                    "Sorry, we couldn't edit this note with that photo", 
                                                    "Please try again", 
                                                    EditNoteActivity.this);
                                        }
                                    }
                                });
                            }
                        } 
                    }
                });
            }
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
                photoUri = data.getData();
                try {
                    mHasChangedPhoto = true;
                    mNotePhoto = MediaStore.Images.Media.getBitmap
                            (this.getContentResolver(), photoUri);
                    Matrix matrix = 
                            PinItUtils.getRotationMatrixForImage(getApplicationContext(), photoUri);
                    mNotePhotoImageView.setAdjustViewBounds(true);

                    mNotePhoto = Bitmap.createScaledBitmap(
                            mNotePhoto, mNotePhoto.getWidth()/2, mNotePhoto.getHeight()/2, true);
                    mNotePhoto = Bitmap.createBitmap(
                            mNotePhoto, 0, 0, mNotePhoto.getWidth(), mNotePhoto.getHeight(), matrix, true);

                    mNotePhotoThumbnail = Bitmap.createScaledBitmap(
                            mNotePhoto, mNotePhoto.getWidth()/4, mNotePhoto.getHeight()/4, true);

                    mNotePhotoImageView.setImageBitmap(mNotePhoto);

                    mNotePhotoCloseImageView.setVisibility(View.VISIBLE);
                    mNotePhotoCloseImageView.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            mHasChangedPhoto = false;
                            mNotePhoto = null;
                            mNotePhotoThumbnail = null;

                            if(mHasPhoto) {
                                mNotePhotoImageView.setImageBitmap(mCurrentPhoto);
                                mNotePhotoCloseImageView.setVisibility(View.INVISIBLE);
                            }
                            else {
                                mNotePhotoImageView.setImageDrawable(
                                        getResources().getDrawable(R.drawable.plus_sign));
                                mNotePhotoCloseImageView.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                } catch (IOException e) {
                    PinItUtils.createAlert("Something's gone wrong", "Try choosing a photo again!",
                            EditNoteActivity.this);
                }	
            }
            break;
        }
    }

    @Override
    protected void onDestroy() {
        if(mCurrentPhoto != null) {
            mCurrentPhoto.recycle();
            mCurrentPhoto = null;
        }
        if(mNotePhoto != null) {
            mNotePhoto.recycle();
            mNotePhoto = null;
        }
        if(mNotePhotoThumbnail != null) {
            mNotePhotoThumbnail.recycle();
            mNotePhotoThumbnail = null;
        }
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