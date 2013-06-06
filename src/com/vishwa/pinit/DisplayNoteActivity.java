package com.vishwa.pinit;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
	private ProgressBar mProgressBar;
	
	private Note mNote;
	
	public final static String NOTE_LOAD_ERROR = "Sorry, this note couldn't be loaded. It might " +
			"have been deleted by it's creator";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mNote = (Note) getIntent().getExtras().getParcelable("note");
		
		if(mNote.getNoteImageThumbnailUrl().isEmpty()) {
			setContentView(R.layout.activity_display_note_alt);

			mNoteBodyAlt = (TextView) findViewById(R.id.display_note_body_alt);
			mNoteTitleAlt = (TextView) findViewById(R.id.display_note_title_alt);
			
			mNoteTitleAlt.setVisibility(TextView.GONE);
			mNoteBodyAlt.setVisibility(TextView.GONE);

			mNoteTitleAlt.setText(mNote.getNoteTitle());
			mNoteTitleAlt.setVisibility(TextView.VISIBLE);
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
			
			mNotePhotoImageView.setVisibility(ImageView.GONE);
			mNoteTitle.setVisibility(TextView.GONE);
			mNoteBody.setVisibility(TextView.GONE);
			
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
									Bitmap notePhoto = 
											BitmapFactory.decodeByteArray(data, 0, data.length);
									mProgressBar.setVisibility(ProgressBar.GONE);
									mNotePhotoImageView.setAdjustViewBounds(true);
									mNotePhotoImageView.setImageBitmap(notePhoto);
									mNoteTitle.setText(mNote.getNoteTitle());
									mNotePhotoImageView.setVisibility(ImageView.VISIBLE);
									mNoteTitle.setVisibility(TextView.VISIBLE);
									if(!mNote.getNoteBody().isEmpty()) {
										mNoteBody.setText(mNote.getNoteBody());
										mNoteBody.setVisibility(TextView.VISIBLE);
									}
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}
