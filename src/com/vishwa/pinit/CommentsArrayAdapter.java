package com.vishwa.pinit;

import java.util.List;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class CommentsArrayAdapter extends ArrayAdapter<NoteComment> {
    private ImageLoader mImageLoader;
    
    public CommentsArrayAdapter(Context context, int textViewResourceId, List<NoteComment> objects,
            ImageLoader imageLoader) {
        super(context, textViewResourceId, objects);
        mImageLoader = imageLoader;
    }

    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.comments_list_row, null);
        }
        
        ViewHolder holder = (ViewHolder) v.getTag(R.id.id_holder);       
        
        if (holder == null) {
            holder = new ViewHolder(v);
            v.setTag(R.id.id_holder, holder);
        }        
        
        NoteComment entry = getItem(position);
        if (entry.getThumbnailUrl() != null) {
            holder.mUserPhoto.setImageUrl(entry.getThumbnailUrl(), mImageLoader);
        } else {
            holder.mUserPhoto.setImageResource(R.drawable.default_image);
        }
        
        holder.mCommentText.setText(entry.getCommentText());
        holder.mCommentCreator.setText(entry.getCommentCreator());
        holder.mCommentCreatedAt.setText(entry.getCommentCreatedAt());
        
        return v;
    }
    
    
    private class ViewHolder {
        NetworkImageView mUserPhoto;
        TextView mCommentText; 
        TextView mCommentCreator;
        TextView mCommentCreatedAt;
        
        public ViewHolder(View v) {
            mUserPhoto = (NetworkImageView) v.findViewById(R.id.comments_comment_creator_photo);
            mCommentText = (TextView) v.findViewById(R.id.comments_comment_text);
            mCommentCreator = (TextView) v.findViewById(R.id.comments_comment_creator);
            mCommentCreatedAt = (TextView) v.findViewById(R.id.comments_comment_created_time);
            
            mUserPhoto.setDefaultImageResId(R.drawable.default_image);
            
            v.setTag(this);
        }
    }
}