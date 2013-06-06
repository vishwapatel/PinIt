package com.vishwa.pinit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

public class SignupActivity extends Activity {
	
	public static final int REQUEST_CODE_PHOTO_SELECT = 101;
	
	private ImageView mPhotoImageView;
	private Button mProfilePhotoButton;
	private EditText mUsernameField;
	private EditText mPasswordField;
	private EditText mConfirmPasswordField;
	private Button mConfirmSignupButton;
	private Button mCancelButton;
	private ProgressBar mProgressBar;
	
	private boolean mIsDefaultPhoto = true;
	private String mUsername;
	
	private Bitmap mProfilePhoto;
	private Bitmap mProfilePhotoThumbnail;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_signup);
		
		mPhotoImageView = (ImageView) findViewById(R.id.signup_photo);
		mProfilePhotoButton = (Button) findViewById(R.id.signup_photo_button);
		mUsernameField = (EditText) findViewById(R.id.signup_username_field);
		mPasswordField = (EditText) findViewById(R.id.signup_password_field);
		mConfirmPasswordField = (EditText) findViewById(R.id.signup_confirm_password_field);
		mConfirmSignupButton = (Button) findViewById(R.id.signup_confirm_signup_button);
		mCancelButton = (Button) findViewById(R.id.signup_cancel_button);
		mProgressBar = (ProgressBar) findViewById(R.id.signup_progressBar);
		
		mProgressBar.setVisibility(View.INVISIBLE);
		
		mProfilePhoto = BitmapFactory.decodeResource(getResources(), R.drawable.default_image);
		mProfilePhotoThumbnail = ThumbnailUtils.extractThumbnail(mProfilePhoto, 100, 100);
		
		mProfilePhotoButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
	        	Intent intent = new Intent(Intent.ACTION_PICK,
	            						   android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
	                startActivityForResult(intent, REQUEST_CODE_PHOTO_SELECT);
			}
		});
		
		mConfirmSignupButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isEmpty(mUsernameField) 
						|| isEmpty(mPasswordField) 
						|| isEmpty(mConfirmPasswordField)) {
					PinItUtils.createAlert("You've missed something!", 
										   "You've left one of the fields empty.", 
										   SignupActivity.this);
				}
				else if(mPasswordField.getText().length() < 6) {
					mPasswordField.setError("Password should be at least 6 characters");
					PinItUtils.createAlert("Your password is too short!", 
										   "The password must be 6 characters or more", 
										   SignupActivity.this);
				}
				else if(!mPasswordField.getText().toString().equals(
						mConfirmPasswordField.getText().toString())) {
					PinItUtils.createAlert("Your passwords don't match!", 
							               "Please re-type the passwords", 
							               SignupActivity.this);
				}
				else {
					
					mProgressBar.setVisibility(View.VISIBLE);
					
					mUsername = mUsernameField.getText().toString();
					
					final ParseUser user = new ParseUser();
					user.setUsername(mUsernameField.getText().toString());
					user.setPassword(mPasswordField.getText().toString());
					user.put("isDefaultPhoto", mIsDefaultPhoto);
					
					if(mIsDefaultPhoto) {
						signupNewUser(user);
					}
					else {
						ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
						mProfilePhotoThumbnail.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
						byte[] photoBytes = byteStream.toByteArray();
						
						CachePhotoTask cachePhotoTask = new CachePhotoTask();
						cachePhotoTask.execute();
						
						final ParseFile userPhotoThumbnail = 
								new ParseFile("photoThumbnail.png", photoBytes);
						
						userPhotoThumbnail.saveInBackground(new SaveCallback() {
							
							@Override
							public void done(ParseException e) {
								if(e == null) {
									user.put("profilePhotoThumbnail", userPhotoThumbnail);
									signupNewUser(user);
								}
								else {
									String error = e.getMessage().substring(0, 1).toUpperCase( )+ 
												   e.getMessage().substring(1);
									PinItUtils.createAlert("Sorry, we couldn't save this photo", 
											error, SignupActivity.this);
								}
							}
						});
					}
					
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
	
	private void signupNewUser(ParseUser user) {
		
		user.signUpInBackground(new SignUpCallback() {
			
			@Override
			public void done(ParseException e) {
				
				mProgressBar.setVisibility(View.INVISIBLE);
				
				if(e == null) {
					Intent intent = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intent);
					finish();
				}
				else {
					String error = e.getMessage().substring(0, 1).toUpperCase()+ 
							e.getMessage().substring(1);
					
					PinItUtils.createAlert("Sign up failed", error, SignupActivity.this);
				}
			}
		});
	}
	
	class CachePhotoTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			String filename = mUsername + ".png";
			
			if( mProfilePhotoThumbnail != null) {
				try {
					FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
					mProfilePhotoThumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
					outputStream.close();
				}
				catch (IOException e) {
					PinItUtils.createAlert("This is embarrassing", 
							"Something's gone wrong, try signing up again.", SignupActivity.this);
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
        	mIsDefaultPhoto = false;
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
						"Try choosing a photo again!", SignupActivity.this);
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
		super.onDestroy();
	}

	
}
