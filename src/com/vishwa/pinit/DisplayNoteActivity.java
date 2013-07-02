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
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class DisplayNoteActivity extends Activity {

    public final static int NOTE_COMMENTS_DISPLAY = 106;
    public final static String NOTE_LOAD_ERROR = "Sorry, this note couldn't be loaded. It might " +
            "have been deleted by it's creator";

    private ImageView mNotePhotoImageView;
    private TextView mNoteTitle;
    private TextView mNoteBody;
    private TextView mLikesAndCommentsInfo;
    private TextView mNoteCreatedInfo;
    private ImageButton mLikeButton;
    private ImageButton mCommentButton;
    private ProgressBar mProgressBar;
    private RelativeLayout mUserInfoLayout;
    private ScrollView mScrollView;

    private Bitmap mNotePhoto = null;

    protected static ParseObject mParseNote;

    private Note mNote;
    private ArrayList<NoteComment> mNoteComments = new ArrayList<NoteComment>();
    private boolean mUserLikesNote = false;
    private boolean mShowingComments = false;
    private int mNumberOfCommentsLoaded = 0;
    private int mNoteLikesCount = 0;
    private int mNoteCommentsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mNote = (Note) getIntent().getExtras().getParcelable("note");
        byte[] array = getIntent().getByteArrayExtra("userPhoto");
        Bitmap userPhoto = Bitmap.createScaledBitmap(
                BitmapFactory.decodeByteArray(array, 0, array.length), 
                (int) getResources().getDimension(R.dimen.display_user_photo_radius), 
                (int) getResources().getDimension(R.dimen.display_user_photo_radius), 
                true);
        Bitmap frame = BitmapFactory.decodeResource(getResources(), R.drawable.circular_frame);
        frame = Bitmap.createScaledBitmap(frame, 
                (int) getResources().getDimension(R.dimen.display_user_photo_radius), 
                (int) getResources().getDimension(R.dimen.display_user_photo_radius), 
                true);
        Canvas canvas = new Canvas(userPhoto);
        canvas.drawBitmap(frame, 0.0f, 0.0f, null);
        BitmapDrawable userPhotoDrawable = new BitmapDrawable(getResources(), userPhoto);

        if(mNote.getNoteImageThumbnailUrl().isEmpty()) {
            setContentView(R.layout.activity_display_note_alt);

            mNoteBody = (TextView) findViewById(R.id.display_note_body_alt);
            mNoteTitle = (TextView) findViewById(R.id.display_note_title_alt);
            mNoteCreatedInfo = (TextView) findViewById(R.id.display_note_userinfo_alt);
            mLikeButton = (ImageButton) findViewById(R.id.display_note_like_alt);
            mCommentButton = (ImageButton) findViewById(R.id.display_note_comment_alt);
            mLikesAndCommentsInfo = (TextView) findViewById(R.id.display_note_likes_comments_info_alt);
            mNoteCreatedInfo.setCompoundDrawablesWithIntrinsicBounds(userPhotoDrawable, null, null, null);

            mNoteTitle.setVisibility(TextView.GONE);
            mNoteBody.setVisibility(TextView.GONE);
            mLikesAndCommentsInfo.setVisibility(TextView.GONE);

            mLikeButton.setEnabled(false);
            mNoteTitle.setText(mNote.getNoteTitle());
            mNoteTitle.setVisibility(TextView.VISIBLE);
            mNoteCreatedInfo.setText("Created by "+mNote.getNoteCreator()+" on "+mNote.getNoteCreatedAt());
            mNoteCreatedInfo.setVisibility(TextView.VISIBLE);
            if(!mNote.getNoteBody().isEmpty()) {
                mNoteBody.setText(mNote.getNoteBody());
                mNoteBody.setVisibility(TextView.VISIBLE);
            }
        }
        else {
            setContentView(R.layout.activity_display_note);	
            mNotePhotoImageView = (ImageView) findViewById(R.id.display_note_photo);
            mProgressBar = (ProgressBar) findViewById(R.id.display_note_progressbar);
            mNoteBody = (TextView) findViewById(R.id.display_note_body);
            mNoteTitle = (TextView) findViewById(R.id.display_note_title);
            mNoteCreatedInfo = (TextView) findViewById(R.id.display_note_userinfo);
            mLikeButton = (ImageButton) findViewById(R.id.display_note_like);
            mCommentButton = (ImageButton) findViewById(R.id.display_note_comment);
            mLikesAndCommentsInfo = (TextView) findViewById(R.id.display_note_likes_comments_info);
            mUserInfoLayout = (RelativeLayout) findViewById(R.id.display_note_userinfo_layout);
            mScrollView = (ScrollView) findViewById(R.id.display_scroll_layout);
            mNoteCreatedInfo.setCompoundDrawablesWithIntrinsicBounds(userPhotoDrawable, null, null, null);

            mNotePhotoImageView.setVisibility(ImageView.GONE);
            mNoteTitle.setVisibility(TextView.GONE);
            mNoteBody.setVisibility(TextView.GONE);
            mLikesAndCommentsInfo.setVisibility(TextView.GONE);
            mUserInfoLayout.setVisibility(RelativeLayout.GONE);

            mLikeButton.setEnabled(false);
        }

        ParseQuery query = new ParseQuery("Note");
        query.getInBackground(mNote.getNoteId(), new GetCallback() {

            @Override
            public void done(final ParseObject object, ParseException e) {
                if(e == null) {
                    mParseNote = object;

                    loadLikesAndCommentsCounts();
                    loadUserLikePreference();
                    loadCommentsInBackground();

                    if(!mNote.getNoteImageThumbnailUrl().isEmpty()) {
                        ParseFile notePhotoFile = mParseNote.getParseFile("notePhoto");
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
                                    mLikesAndCommentsInfo.setVisibility(TextView.VISIBLE);
                                    mNoteTitle.setVisibility(TextView.VISIBLE);
                                    mUserInfoLayout.setVisibility(RelativeLayout.VISIBLE);
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
                }
                else {
                    Toast.makeText(getApplicationContext(), NOTE_LOAD_ERROR, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        mLikesAndCommentsInfo.setOnClickListener(new OnCommentClickListener());
        mCommentButton.setOnClickListener(new OnCommentClickListener());    
        mLikeButton.setOnClickListener(new OnLikeClickListener());
    }

    private class OnCommentClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            mShowingComments = true;
            Intent intent = new Intent(getApplicationContext(), CommentsActivity.class);
            intent.putExtra("userLikesNote", mUserLikesNote);
            intent.putExtra("noteLikesCount", mNoteLikesCount);
            intent.putExtra("noteCommentsCount", mNoteCommentsCount);
            intent.putExtra("numberOfCommentsLoaded", mNumberOfCommentsLoaded);
            intent.putParcelableArrayListExtra("noteComments", mNoteComments);
            startActivityForResult(intent,NOTE_COMMENTS_DISPLAY);
        }

    }

    private class OnLikeClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if(mUserLikesNote) {
                mUserLikesNote = false;
                mLikeButton.setImageResource(R.drawable.heart);
                mLikeButton.setEnabled(false);
                mNoteLikesCount--;
                updateLikesAndCommentsSize();
                final ParseRelation relation = mParseNote.getRelation("likes");
                ParseQuery query = new ParseQuery("Like");
                query.whereEqualTo("creator", ParseUser.getCurrentUser());
                query.whereEqualTo("noteId", mParseNote.getObjectId());
                query.getFirstInBackground(new GetCallback() {

                    @Override
                    public void done(final ParseObject object, ParseException e) {
                        if(e == null) {
                            relation.remove(object);
                            mParseNote.saveInBackground(new SaveCallback() {

                                @Override
                                public void done(ParseException e) {
                                    if(e == null) {
                                        object.deleteInBackground(new DeleteCallback() {

                                            @Override
                                            public void done(ParseException e) {
                                                if(e == null) {
                                                    mLikeButton.setEnabled(true);
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
            else {
                mUserLikesNote = true;
                mLikeButton.setImageResource(R.drawable.heart_red);
                mLikeButton.setEnabled(false);
                mNoteLikesCount++;
                updateLikesAndCommentsSize();
                final ParseRelation relation = mParseNote.getRelation("likes");
                final ParseObject like = new ParseObject("Like");
                like.put("creator", ParseUser.getCurrentUser());
                like.put("creatorName", ParseUser.getCurrentUser().getUsername());
                like.put("noteId", mParseNote.getObjectId());
                if(ParseUser.getCurrentUser().getBoolean("isDefaultPhoto")) {
                    like.put("creatorPhotoUrl", "http://s3.amazonaws.com/pinit/default_image.png");
                }
                else {
                    like.put("creatorPhotoUrl", 
                            ParseUser.getCurrentUser()
                            .getParseFile("profilePhotoThumbnail")
                            .getUrl());
                }
                like.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if(e == null) {
                            relation.add(like);
                            mParseNote.saveInBackground(new SaveCallback() {

                                @Override
                                public void done(ParseException e) {
                                    mLikeButton.setEnabled(true);
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    private void loadLikesAndCommentsCounts() {
        ParseQuery likesCountQuery = mParseNote.getRelation("likes").getQuery();
        likesCountQuery.countInBackground(new CountCallback() {

            @Override
            public void done(int count, ParseException e) {
                if(e == null) {
                    mNoteLikesCount = count;
                    ParseQuery commentsCountQuery = mParseNote.getRelation("comments").getQuery();
                    commentsCountQuery.countInBackground(new CountCallback() {

                        @Override
                        public void done(int count, ParseException e) {
                            if(e == null) {
                                mNoteCommentsCount = count;
                                updateLikesAndCommentsSize();

                                if(mNote.getNoteImageThumbnailUrl().isEmpty()) {
                                    mLikesAndCommentsInfo.setVisibility(TextView.VISIBLE);    
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadUserLikePreference() {
        ParseQuery likeQuery = new ParseQuery("Like");
        likeQuery.whereEqualTo("creator", ParseUser.getCurrentUser());
        likeQuery.whereEqualTo("noteId", mParseNote.getObjectId());
        likeQuery.getFirstInBackground(new GetCallback() {

            @Override
            public void done(ParseObject object, ParseException e) {
                mLikeButton.setEnabled(true);
                if(e == null && object != null) {
                    mUserLikesNote = true;
                    mLikeButton.setImageResource(R.drawable.heart_red);
                }
                else if(object == null) {
                    mUserLikesNote = false;
                    mLikeButton.setImageResource(R.drawable.heart);
                }
            }
        });
    }

    private void updateLikesAndCommentsSize() {
        if(mNoteCommentsCount == 0 && mNoteLikesCount == 0) {
            mLikesAndCommentsInfo.setText("Be the first to like this!");
        }
        else if(mNoteCommentsCount == 0) {
            if(mNoteLikesCount == 1) {
                mLikesAndCommentsInfo.setText("1 Like");
            }
            else {
                mLikesAndCommentsInfo.setText(String.format("%d Likes", mNoteLikesCount));
            }
        }
        else if(mNoteLikesCount == 0) {
            if(mNoteCommentsCount == 1) {
                mLikesAndCommentsInfo.setText("1 Comment");
            }
            else {
                mLikesAndCommentsInfo.setText(String.format("%d Comments", mNoteCommentsCount));
            }
        }
        else {
            if(mNoteLikesCount == 1 && mNoteCommentsCount == 1) {
                mLikesAndCommentsInfo.setText("1 Like \u00b7 1 Comment");
            }
            else if(mNoteLikesCount == 1) {
                mLikesAndCommentsInfo.setText(String.format("1 Like \u00b7 %d Comments", mNoteCommentsCount));
            }
            else if(mNoteCommentsCount == 1) {
                mLikesAndCommentsInfo.setText(String.format("%d Likes \u00b7 1 Comment", mNoteLikesCount));
            }
            else {
                mLikesAndCommentsInfo.setText(
                        String.format("%d Likes \u00b7 %d Comments", mNoteLikesCount, mNoteCommentsCount));
            }
        }
    }

    private void loadCommentsInBackground() {
        if(!mShowingComments) {
            ParseQuery commentsQuery = mParseNote.getRelation("comments").getQuery();
            commentsQuery.setLimit(10);
            commentsQuery.setSkip(mNumberOfCommentsLoaded);
            commentsQuery.addAscendingOrder("createdAt");
            commentsQuery.findInBackground(new FindCallback() {

                @SuppressLint("SimpleDateFormat")
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        for(ParseObject comment: objects) {
                            String date = comment.getCreatedAt().toString();
                            NoteComment newComment = new NoteComment(
                                    comment.getString("creatorName"), 
                                    comment.getString("commentText"), 
                                    comment.getString("creatorPhotoUrl"),
                                    PinItUtils.getFormattedCommentCreatedAt(date),
                                    comment.getCreatedAt().toString());
                            if(!mNoteComments.contains(newComment)) {
                                mNoteComments.add(newComment);
                                mNumberOfCommentsLoaded++;
                            }
                        }
                    }
                    else {
                        //We can fail silently here because we are only pre-fetching the comments to make
                        //the loading time as close to zero as possible, and anyway, we will reload these
                        //comments in the CommentsActivity if this fails and we will show an error there.
                    }
                    if(objects != null && objects.size() != 0) {
                        loadCommentsInBackground();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
        case NOTE_COMMENTS_DISPLAY:
            if(resultCode == RESULT_OK) {
                mUserLikesNote = data.getBooleanExtra("userLikesNote", false);
                mNoteLikesCount = data.getIntExtra("noteLikesCount", 0);
                if(mUserLikesNote) {
                    mLikeButton.setImageResource(R.drawable.heart_red);
                }
                else {
                    mLikeButton.setImageResource(R.drawable.heart);
                }
                mNumberOfCommentsLoaded = data.getIntExtra("numberOfCommentsLoaded", 0);
                if(mNoteCommentsCount < data.getIntExtra("noteCommentsCount", mNoteCommentsCount)) {
                    mNoteCommentsCount = data.getIntExtra("noteCommentsCount", mNoteCommentsCount);
                }
                ArrayList<NoteComment> comments = data.getParcelableArrayListExtra("noteComments");
                mNoteComments = comments;
                updateLikesAndCommentsSize();
            }
        }
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
