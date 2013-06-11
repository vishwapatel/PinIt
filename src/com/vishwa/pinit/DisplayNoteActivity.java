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
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;


public class DisplayNoteActivity extends Activity {

    private ImageView mNotePhotoImageView;
    private TextView mNoteTitle;
    private TextView mNoteTitleAlt;
    private TextView mNoteBody;
    private TextView mNoteBodyAlt;
    private TextView mNoteCreatedInfo;
    private ProgressBar mProgressBar;
    private ScrollView mScrollView;

    private Bitmap mNotePhoto = null;

    private Note mNote;

    public final static String NOTE_LOAD_ERROR = "Sorry, this note couldn't be loaded. It might " +
            "have been deleted by it's creator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mNote = (Note) getIntent().getExtras().getParcelable("note");
        byte[] array = getIntent().getByteArrayExtra("userPhoto");
        Bitmap bitmap = getCroppedBitmap(BitmapFactory.decodeByteArray(array, 0, array.length));
        Bitmap userPhoto = Bitmap.createScaledBitmap(bitmap, 40, 40, true);
        bitmap.recycle();
        Drawable userPhotoDrawable = new BitmapDrawable(getResources(), userPhoto);

        if(mNote.getNoteImageThumbnailUrl().isEmpty()) {
            setContentView(R.layout.activity_display_note_alt);

            mNoteBodyAlt = (TextView) findViewById(R.id.display_note_body_alt);
            mNoteTitleAlt = (TextView) findViewById(R.id.display_note_title_alt);
            mNoteCreatedInfo = (TextView) findViewById(R.id.display_note_userinfo_alt);
            mNoteCreatedInfo.setCompoundDrawablesWithIntrinsicBounds(userPhotoDrawable, null, null, null);

            mNoteTitleAlt.setVisibility(TextView.GONE);
            mNoteBodyAlt.setVisibility(TextView.GONE);
            mNoteCreatedInfo.setVisibility(TextView.GONE);

            mNoteTitleAlt.setText(mNote.getNoteTitle());
            mNoteTitleAlt.setVisibility(TextView.VISIBLE);
            mNoteCreatedInfo.setText("Created by "+mNote.getNoteCreator()+" on "+mNote.getNoteCreatedAt());
            mNoteCreatedInfo.setVisibility(TextView.VISIBLE);
            if(!mNote.getNoteBody().isEmpty()) {
                mNoteBodyAlt.setText(mNote.getNoteBody());
                mNoteBodyAlt.setVisibility(TextView.VISIBLE);
            }
        }
        else {
            setContentView(R.layout.activity_display_note);	
            mNotePhotoImageView = (ImageView) findViewById(R.id.display_note_photo);
            mProgressBar = (ProgressBar) findViewById(R.id.display_note_progressbar);
            mNoteBody = (TextView) findViewById(R.id.display_note_body);
            mNoteTitle = (TextView) findViewById(R.id.display_note_title);
            mNoteCreatedInfo = (TextView) findViewById(R.id.display_note_userinfo);
            mScrollView = (ScrollView) findViewById(R.id.display_scroll_layout);
            mNoteCreatedInfo.setCompoundDrawablesWithIntrinsicBounds(userPhotoDrawable, null, null, null);

            mNotePhotoImageView.setVisibility(ImageView.GONE);
            mNoteTitle.setVisibility(TextView.GONE);
            mNoteBody.setVisibility(TextView.GONE);
            mNoteCreatedInfo.setVisibility(TextView.GONE);

            ParseQuery query = new ParseQuery("Note");
            query.getInBackground(mNote.getNoteId(), new GetCallback() {

                @Override
                public void done(ParseObject object, ParseException e) {
                    if(e == null) {
                        ParseFile notePhotoFile = object.getParseFile("notePhoto");
                        mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        notePhotoFile.getDataInBackground(new GetDataCallback() {

                            @Override
                            public void done(byte[] data, ParseException e) {
                                if(e == null) {
                                    mNotePhoto = 
                                            BitmapFactory.decodeByteArray(data, 0, data.length);
                                    mProgressBar.setVisibility(ProgressBar.GONE);
                                    mNotePhotoImageView.setAdjustViewBounds(true);
                                    mNotePhotoImageView.setImageBitmap(mNotePhoto);
                                    mNoteTitle.setText(mNote.getNoteTitle());
                                    mNoteCreatedInfo.setText("Created by "+mNote.getNoteCreator()+
                                            " on "+mNote.getNoteCreatedAt());
                                    mNotePhotoImageView.setVisibility(ImageView.VISIBLE);
                                    mNoteTitle.setVisibility(TextView.VISIBLE);
                                    mNoteCreatedInfo.setVisibility(TextView.VISIBLE);
                                    if(!mNote.getNoteBody().isEmpty()) {
                                        mNoteBody.setText(mNote.getNoteBody());
                                        mNoteBody.setVisibility(TextView.VISIBLE);
                                    }
                                    ViewTreeObserver viewObserver = mScrollView.getViewTreeObserver();
                                    viewObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

                                        @SuppressWarnings("deprecation")
                                        @Override
                                        public void onGlobalLayout() {
                                            if(mScrollView.canScrollVertically(1)) {
                                                mScrollView.post(new Runnable() { 
                                                    public void run() { 
                                                        int halfWay = mScrollView.getHeight()/2;
                                                        mScrollView.scrollTo(0, halfWay);
                                                    } 
                                                });
                                            }
                                            mScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                        }
                                    });
                                }
                            }
                        });
                    }
                    else {
                        Toast.makeText(getApplicationContext(), NOTE_LOAD_ERROR, Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        }
    }


    private Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    @Override
    protected void onDestroy() {
        if(mNotePhoto != null) {
            mNotePhoto.recycle();
            mNotePhoto = null;
        }
        super.onDestroy();
    }

}
