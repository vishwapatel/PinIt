package com.vishwa.pinit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class CommentsActivity extends Activity {

    public final static int DISPLAY_NOTE_LIKES = 107;

    private Button mSendCommentButton;
    private ImageButton mLikeButton;
    private ImageButton mShowLikesButton;
    private EditText mCommentField;
    private TextView mNumberOfLikes;
    private ListView mCommentsListView;

    private ParseObject mParseNote = DisplayNoteActivity.mParseNote;

    private int mNoteLikesCount;
    private int mNoteCommentsCount;
    private int mNumberOfCommentsLoaded = 0;
    private int mNumberOfLikesLoaded = 0;
    private boolean mUserLikesNote;
    private boolean mShowingLikes;
    private ArrayList<NoteComment> mNoteComments = new ArrayList<NoteComment>();
    private ArrayList<NoteLike> mNoteLikes = new ArrayList<NoteLike>();
    private CommentsArrayAdapter mAdapter;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_comments_display);

        mSendCommentButton = (Button) findViewById(R.id.comments_send_comment);
        mLikeButton = (ImageButton) findViewById(R.id.comments_like_button);
        mShowLikesButton = (ImageButton) findViewById(R.id.comments_show_likes);
        mCommentField = (EditText) findViewById(R.id.comments_new_comment);
        mNumberOfLikes = (TextView) findViewById(R.id.comments_number_of_likes);
        mCommentsListView = (ListView) findViewById(R.id.comments_listview);

        mAdapter = new CommentsArrayAdapter(
                this, 0, mNoteComments, ((PinItApplication)getApplication()).getImageLoader());
        mCommentsListView.setAdapter(mAdapter);
        mCommentsListView.setDivider(null);
        mCommentsListView.setDividerHeight(0);
        mCommentsListView.setOnScrollListener(new EndlessScrollListener());

        Intent intent = getIntent();
        mNoteLikesCount = intent.getIntExtra("noteLikesCount", 0);
        mNoteCommentsCount = intent.getIntExtra("noteCommentsCount", 0);
        mUserLikesNote = intent.getBooleanExtra("userLikesNote", false);
        mNumberOfCommentsLoaded = intent.getIntExtra("numberOfCommentsLoaded", 0);
        ArrayList<NoteComment> comments = intent.getParcelableArrayListExtra("noteComments");
        for(NoteComment comment : comments) {
            mNoteComments.add(comment);
        }
        mAdapter.notifyDataSetChanged();
        setActivityResult();

        if(mNoteCommentsCount > 0) {
            mCommentsListView.setBackgroundDrawable(null);
        }

        if(mUserLikesNote) {
            mLikeButton.setImageResource(R.drawable.heart_red);
        }
        else {
            mLikeButton.setImageResource(R.drawable.heart);
        }
        updateLikesSize();

        loadLikesInBackground();
        loadMoreComments();

        mNumberOfLikes.setOnClickListener(new OnShowLikesButtonClickListener());
        mShowLikesButton.setOnClickListener(new OnShowLikesButtonClickListener());

        mLikeButton.setOnClickListener(new OnLikeButtonClickListener());  
        mSendCommentButton.setOnClickListener(new OnCommentButtonClickListener());
    }

    public class OnLikeButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View likeButtonView) {
            if(mUserLikesNote) {
                mUserLikesNote = false;
                mLikeButton.setImageResource(R.drawable.heart);
                mLikeButton.setEnabled(false);
                mNoteLikesCount--;
                updateLikesSize();
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
                                                    Iterator<NoteLike> iterator = mNoteLikes.iterator();
                                                    while(iterator.hasNext()) {
                                                        NoteLike like = iterator.next();
                                                        if(like.getCommentCreator().equals(
                                                                ParseUser.getCurrentUser().getUsername())) {
                                                            iterator.remove();
                                                            break;
                                                        }
                                                    }
                                                    setActivityResult();
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
                final Animation likeButtonAnimation = 
                        AnimationUtils.loadAnimation(CommentsActivity.this, R.anim.like_button_anim);
                likeButtonView.startAnimation(likeButtonAnimation);
                mLikeButton.setEnabled(false);
                mNoteLikesCount++;
                updateLikesSize();
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
                                    NoteLike like;
                                    if(ParseUser.getCurrentUser().getBoolean("isDefaultPhoto")) {
                                        like = new NoteLike(
                                                ParseUser.getCurrentUser().getUsername(), 
                                                "http://s3.amazonaws.com/pinit/default_image.png",
                                                mParseNote.getObjectId());
                                    }
                                    else {
                                        like = new NoteLike(
                                                ParseUser.getCurrentUser().getUsername(), 
                                                ParseUser.getCurrentUser()
                                                .getParseFile("profilePhotoThumbnail")
                                                .getUrl(),
                                                mParseNote.getObjectId());
                                    }
                                    mNoteLikes.add(like);
                                    setActivityResult();
                                    mLikeButton.setEnabled(true);
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    public class OnShowLikesButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if(!mNumberOfLikes.getText().toString().equals("Be the first to like this!")) {
                mShowingLikes = true;
                Intent intent = new Intent(getApplicationContext(), LikesActivity.class);
                intent.putExtra("numberofLikesLoaded", mNumberOfLikesLoaded);
                intent.putParcelableArrayListExtra("noteLikes", mNoteLikes);
                startActivityForResult(intent, DISPLAY_NOTE_LIKES);
            }
        }

    }

    public class OnCommentButtonClickListener implements OnClickListener{
        @Override
        public void onClick(View v) {
            if(!mCommentField.getText().toString().isEmpty()) {
                mSendCommentButton.setEnabled(false);
                final ParseRelation relation = mParseNote.getRelation("comments");
                final ParseObject comment = new ParseObject("Comment");
                comment.put("creator", ParseUser.getCurrentUser());
                comment.put("noteId", mParseNote.getObjectId());
                comment.put("commentText", mCommentField.getText().toString());
                comment.put("creatorName", ParseUser.getCurrentUser().getUsername());
                if(ParseUser.getCurrentUser().getBoolean("isDefaultPhoto")) {
                    comment.put("creatorPhotoUrl", "http://s3.amazonaws.com/pinit/default_image.png");
                }
                else {
                    comment.put("creatorPhotoUrl", 
                            ParseUser.getCurrentUser()
                            .getParseFile("profilePhotoThumbnail")
                            .getUrl());
                }
                comment.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if(e == null) {
                            relation.add(comment);
                            mParseNote.saveInBackground(new SaveCallback() {

                                @SuppressWarnings("deprecation")
                                @Override
                                public void done(ParseException e) {
                                    if(e == null) {
                                        mCommentsListView.setSelection(mAdapter.getCount()-1);
                                        mNoteCommentsCount++;
                                        mNumberOfCommentsLoaded++;
                                        mCommentField.setText("");
                                        mSendCommentButton.setEnabled(true);
                                        String date = comment.getCreatedAt().toString();
                                        mNoteComments.add(new NoteComment(
                                                comment.getString("creatorName"), 
                                                comment.getString("commentText"), 
                                                comment.getString("creatorPhotoUrl"),
                                                PinItUtils.getFormattedCommentCreatedAt(date),
                                                comment.getCreatedAt().toString()));
                                        mAdapter.notifyDataSetChanged();
                                        mCommentsListView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mCommentsListView.setSelection(mAdapter.getCount() - 1);
                                            }
                                        });
                                        mCommentsListView.setBackgroundDrawable(null);
                                        setActivityResult();
                                    }
                                    else {
                                        mSendCommentButton.setEnabled(true);
                                        PinItUtils.createAlert("This is embarrassing", 
                                                e.getMessage(), 
                                                CommentsActivity.this);
                                    }
                                }
                            });
                        }
                        else {
                            mSendCommentButton.setEnabled(true);
                            PinItUtils.createAlert("This is embarrassing", 
                                    "We couldn't post that comment at this moment, please try again", 
                                    CommentsActivity.this);
                        }
                    }
                });
            }
        }
    }

    private void updateLikesSize() {
        mShowLikesButton.setVisibility(ImageButton.VISIBLE);
        if(mNoteLikesCount == 0) {
            mNumberOfLikes.setBackgroundResource(R.drawable.custom_comment_button);
            mShowLikesButton.setVisibility(ImageButton.INVISIBLE);
            mNumberOfLikes.setText("Be the first to like this!");
        }
        else if(mNoteLikesCount == 1) {
            mNumberOfLikes.setBackgroundResource(R.drawable.number_of_likes_background);
            mNumberOfLikes.setText("1 person likes this");
        }
        else {
            mNumberOfLikes.setBackgroundResource(R.drawable.number_of_likes_background);
            mNumberOfLikes.setText(mNoteLikesCount+" people like this");
        }
    }

    /**
     * Detects when user is close to the end of the current page and starts loading the next page
     * so the user will not have to wait (that much) for the next entries.
     * 
     * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
     */
    public class EndlessScrollListener implements OnScrollListener {
        // how many entries earlier to start loading next page
        private int visibleThreshold = 5;
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }
        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {

            mNumberOfCommentsLoaded = totalItemCount;

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                loading = true;
                loadMoreComments();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        public int getCurrentPage() {
            return currentPage;
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
        case DISPLAY_NOTE_LIKES:

            mShowingLikes = false;

            if(resultCode == RESULT_OK) {
                mNumberOfLikesLoaded = data.getIntExtra("numberOfLikesLoaded", 0);
                ArrayList<NoteLike> likes = data.getParcelableArrayListExtra("noteLikes");
                mNoteLikes = likes;
                if(mNumberOfLikesLoaded >= mNoteLikesCount) {
                    mNoteLikesCount = mNumberOfLikesLoaded;
                    updateLikesSize();
                }
            }
        }
    }

    private void setActivityResult() {
        Intent intent = new Intent();
        intent.putExtra("userLikesNote", mUserLikesNote);
        intent.putExtra("noteCommentsCount", mNoteCommentsCount);
        intent.putExtra("noteLikesCount", mNoteLikesCount);
        intent.putExtra("numberOfCommentsLoaded", mNumberOfCommentsLoaded);
        intent.putParcelableArrayListExtra("noteComments", mNoteComments);
        setResult(RESULT_OK, intent);
    }

    private void loadMoreComments() {
        ParseQuery commentsQuery = mParseNote.getRelation("comments").getQuery();
        commentsQuery.setLimit(10);
        commentsQuery.setSkip(mNumberOfCommentsLoaded);
        commentsQuery.addAscendingOrder("createdAt");
        commentsQuery.findInBackground(new FindCallback() {

            @SuppressWarnings("deprecation")
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
                            mNoteCommentsCount++;
                            setActivityResult();
                        }
                        if(mCommentsListView.getBackground() != null) {
                            mCommentsListView.setBackgroundDrawable(null);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
                else {
                    PinItUtils.createAlert("We're sorry", 
                            "We couldn't load the comments for this note at this moment, please try again!", 
                            CommentsActivity.this);
                }
            }
        });
    }

    private void loadLikesInBackground() {
        if(!mShowingLikes) {
            ParseQuery likesQuery = mParseNote.getRelation("likes").getQuery();
            likesQuery.setLimit(10);
            likesQuery.setSkip(mNumberOfLikesLoaded);
            likesQuery.addDescendingOrder("createdAt");
            likesQuery.findInBackground(new FindCallback() {

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        for(ParseObject like: objects) {
                            NoteLike newLike = new NoteLike(
                                    like.getString("creatorName"), 
                                    like.getString("creatorPhotoUrl"),
                                    mParseNote.getObjectId());
                            if(!mNoteLikes.contains(newLike)) {
                                mNoteLikes.add(newLike);
                                mNumberOfLikesLoaded++;
                            }
                        }
                    }
                    else {
                        //We can fail silently here because we are only pre-fetching the likes to make
                        //the loading time as close to zero as possible, and anyway, we will reload these
                        //likes in the LikesActivity if this fails and we will show an error there.
                    }
                    if(objects != null && objects.size() != 0) {
                        loadLikesInBackground();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
