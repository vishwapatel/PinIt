package com.vishwa.pinit;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class LikesActivity extends Activity {

    private ListView mLikesListView;
    
    private ParseObject mParseNote = DisplayNoteActivity.mParseNote;
    
    private ArrayList<NoteLike> mNoteLikes = new ArrayList<NoteLike>();
    private LikesArrayAdapter mAdapter;
    private int mNumberOfLikesLoaded = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_likes_display);
  
        mLikesListView = (ListView) findViewById(R.id.likes_listview);
        
        mAdapter = new LikesArrayAdapter(
                this, 0, mNoteLikes, ((PinItApplication)getApplication()).getImageLoader());
        mLikesListView.setAdapter(mAdapter);
        mLikesListView.setDivider(null);
        mLikesListView.setDividerHeight(0);
        
        Intent intent = getIntent();
        mNumberOfLikesLoaded = intent.getIntExtra("numberOfLikesLoaded", 0);
        ArrayList<NoteLike> likes = intent.getParcelableArrayListExtra("noteLikes");
        for(NoteLike like : likes) {
            mNoteLikes.add(like);
        }
        mAdapter.notifyDataSetChanged();
        
        setActivityResult();
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
            
            mNumberOfLikesLoaded = totalItemCount;
            
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                loading = true;
                loadMoreLikes();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
    }
    
    private void setActivityResult() {
        Intent intent = new Intent();
        intent.putExtra("numberOfLikesLoaded", mNumberOfLikesLoaded);
        intent.putParcelableArrayListExtra("noteLikes", mNoteLikes);
        setResult(RESULT_OK, intent);
    }
    
    private void loadMoreLikes() {
        ParseQuery likesQuery = mParseNote.getRelation("likes").getQuery();
        likesQuery.setLimit(10);
        likesQuery.setSkip(mNumberOfLikesLoaded);
        likesQuery.addDescendingOrder("createdAt");
        likesQuery.findInBackground(new FindCallback() {
            
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    for(ParseObject like: objects) {
                        mNoteLikes.add(new NoteLike(
                                like.getString("creatorName"), 
                                like.getString("creatorPhotoUrl")));
                        setActivityResult();
                    }
                    mAdapter.notifyDataSetChanged();
                }
                else {
                    PinItUtils.createAlert("We're sorry", 
                            "We couldn't load the likes for this note at this moment, please try again!", 
                            LikesActivity.this);
                }
            }
        });
    }
    
    

    @Override
    protected void onDestroy() {
        // TODO: DESTROY EVERYTHING!!!
        super.onDestroy();
    }

}