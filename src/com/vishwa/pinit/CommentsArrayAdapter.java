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
            holder.userPhoto.setImageUrl(entry.getThumbnailUrl(), mImageLoader);
        } else {
            holder.userPhoto.setImageResource(R.drawable.default_image);
        }
        
        holder.commentText.setText(entry.getCommentText());
        holder.commentCreator.setText(entry.getCommentCreator());
        holder.commentCreatedAt.setText(entry.getCommentCreatedAt());
        
        return v;
    }
    
    
    private class ViewHolder {
        NetworkImageView userPhoto;
        TextView commentText; 
        TextView commentCreator;
        TextView commentCreatedAt;
        
        public ViewHolder(View v) {
            userPhoto = (NetworkImageView) v.findViewById(R.id.comments_comment_creator_photo);
            commentText = (TextView) v.findViewById(R.id.comments_comment_text);
            commentCreator = (TextView) v.findViewById(R.id.comments_comment_creator);
            commentCreatedAt = (TextView) v.findViewById(R.id.comments_comment_created_time);
            
            v.setTag(this);
        }
    }
}