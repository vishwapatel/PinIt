package com.vishwa.pinit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class MainActivity extends FragmentActivity implements OnMapLongClickListener, OnMarkerDragListener, OnMarkerClickListener{
  
  public static final int REQUEST_CODE_CREATE_NOTE = 102;
  public static final int REQUEST_CODE_DISPLAY_NOTE = 103;

  private Button mAllNotesButton;
  private Button mYourNotesButton;
  
  private GoogleMap mMap;
  private Menu mMenu;
  private Marker mFocusedMarker;
  
  private Bitmap mUserPhotoThumbnail;
  private LruCache<String, Bitmap> mMemoryCache;
  
  private String mCurrentUsername;
  private ArrayList<Marker> mMarkerList = new ArrayList<Marker>(20);
  private HashMap<String, Note> mNoteStore = new HashMap<String, Note>();
  private HashMap<String, String> mReverseNoteStore = new HashMap<String, String>();
  private boolean mHasInternet = true;
  private MapViewMode mapMode = MapViewMode.ALL_NOTES;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    
    final int cacheSize = maxMemory / 10;
  
    RetainFragment mRetainFragment =
            RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager());
    mMemoryCache = mRetainFragment.mRetainedCache;
    if(mMemoryCache == null) {
	    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            return bitmap.getByteCount() / 1024;
	        }
	    };
	    mRetainFragment.mRetainedCache = mMemoryCache;
    }
    
    mAllNotesButton = (Button) findViewById(R.id.main_all_notes_button);
    mYourNotesButton = (Button) findViewById(R.id.main_your_notes_button);
    
    mAllNotesButton.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LatLngBounds mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;	
			LatLng southwest = mapBounds.southwest;
			LatLng northeast = mapBounds.northeast;
			
			mapMode = MapViewMode.ALL_NOTES;
			
			for(Marker marker: mMarkerList) {
				marker.remove();
			}
			
			mMarkerList.clear();
			mNoteStore.clear();
			mReverseNoteStore.clear();
			
			LatLngTuple tuple = new LatLngTuple(southwest, northeast);
			LoadNotesTask currentUserNotesTask = new LoadNotesTask(tuple, false);
			currentUserNotesTask.execute();
		}
	});
    
    mYourNotesButton.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LatLngBounds mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;	
			LatLng southwest = mapBounds.southwest;
			LatLng northeast = mapBounds.northeast;
			
			for(Marker marker: mMarkerList) {
				marker.remove();
			}
			
			mMarkerList.clear();
			mNoteStore.clear();
			mReverseNoteStore.clear();
			
			mapMode = MapViewMode.YOUR_NOTES;
			LatLngTuple tuple = new LatLngTuple(southwest, northeast);
			LoadNotesTask currentUserNotesTask = new LoadNotesTask(tuple, true);
			currentUserNotesTask.execute();
		}
	});
    
    if(!PinItUtils.isOnline(getApplicationContext())) {
    	PinItUtils.createAlert("Internet connection not found.", 
    			"Connect to the Internet and press the refresh button at the top", this);
    	mHasInternet = false;
    }
    else {
    	mHasInternet = true;
    	loadMapWhenOnline();
    }
  }
  
  private void loadMapWhenOnline() {
	  setUpMapIfNeeded();
	  
	  if(mMemoryCache.get("defaultPhoto") == null) {
			Bitmap defaultPhoto = ThumbnailUtils.extractThumbnail(
					  BitmapFactory.decodeResource(getResources(), R.drawable.default_image), 
					  100, 100);
			mMemoryCache.put("defaultPhoto", defaultPhoto);
		}
	  
	  LoadCurrentUserPhotoTask loadUserPhotoTask = new LoadCurrentUserPhotoTask();
	  loadUserPhotoTask.execute();
	    
	  mCurrentUsername = ParseUser.getCurrentUser().getUsername();
	    
	  mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
			
		@Override
		public void onCameraChange(CameraPosition position) {
			
			if(!PinItUtils.isOnline(getApplicationContext())) {
		    	PinItUtils.createAlert("Internet connection not found.", 
		    			"Connect to the Internet and press the refresh button at the top", 
		    			MainActivity.this);
		    	mHasInternet = false;
    	    	hideNonRefreshMenuItems();
    	
    	    	mMap.getUiSettings().setAllGesturesEnabled(false);
    	    	mMap.getUiSettings().setZoomControlsEnabled(false);
    	    	mMap.getUiSettings().setZoomGesturesEnabled(false);
		    }
			else {
				mHasInternet = true;
			
				LatLngBounds mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;	
				LatLng southwest = mapBounds.southwest;
				LatLng northeast = mapBounds.northeast;
				
				Iterator<Marker> iterator = mMarkerList.iterator();
				while(iterator.hasNext()) {
					Marker marker = iterator.next();
					if(!mapBounds.contains(marker.getPosition())) {
						Note note = mNoteStore.get(marker.getId());
						mNoteStore.remove(marker.getId());
						mReverseNoteStore.remove(note.getNoteId());
						marker.remove();
						iterator.remove();
					}
				}
				
				if(mapMode == MapViewMode.YOUR_NOTES) {
					LatLngTuple tuple = new LatLngTuple(southwest, northeast);
					LoadNotesTask currentUserNotesTask = new LoadNotesTask(tuple, true);
					currentUserNotesTask.execute();
				}
				else {
					LatLngTuple tuple = new LatLngTuple(southwest, northeast);
					LoadNotesTask currentUserNotesTask = new LoadNotesTask(tuple, false);
					currentUserNotesTask.execute();
				}
				Log.d("vishwa", "mMarkerlist Size: " + mMarkerList.size());
				Log.d("vishwa", "mNoteStore Size: " + mNoteStore.size());
			}
		}
			
	});
  }
  
  private void setUpMapIfNeeded() {
      if (mMap == null) {
          mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                  .getMap();
          if (mMap != null) {
              setUpMap();
          }
      }
  }

  private void setUpMap() {
      mMap.setOnMapLongClickListener(this);
      mMap.setOnMarkerClickListener(this);
      mMap.getUiSettings().setCompassEnabled(false);
      mMap.getUiSettings().setRotateGesturesEnabled(false);
  }
  
  class LoadCurrentUserPhotoTask extends AsyncTask<Void, Boolean, Void> {
	  	
		@Override
		protected Void doInBackground(Void... params) {
			if(ParseUser.getCurrentUser().getBoolean("isDefaultPhoto")) {
				  mUserPhotoThumbnail = ThumbnailUtils.extractThumbnail(
						  BitmapFactory.decodeResource(getResources(), R.drawable.default_image), 
						  100, 100);
			}
			else {
				try {
					FileInputStream inputStream = openFileInput(mCurrentUsername + ".png");
					mUserPhotoThumbnail = BitmapFactory.decodeStream(inputStream);
				}
				catch(FileNotFoundException e) {
					loadAndCacheProfilePicture();
				}
			}
			
			return null;	
		}
		
		private void loadAndCacheProfilePicture() {
			ParseFile userPhotoFile = 
					ParseUser.getCurrentUser().getParseFile("profilePhotoThumbnail");
	        userPhotoFile.getDataInBackground(new GetDataCallback() {
					
		      @Override
		      public void done(byte[] data, ParseException e) {
		    	if(e == null) {
		    		mUserPhotoThumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
		    		try {
						FileOutputStream outputStream = 
								openFileOutput(mCurrentUsername, Context.MODE_PRIVATE);
						mUserPhotoThumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
						outputStream.close();
						publishProgress(true);
		    		}
		    		catch(FileNotFoundException e1) {
		    			publishProgress(false);
		    		}
		    		catch(IOException e1) {
		    			publishProgress(false);
		    		}
				}
		    	else {
		    		Log.e("vishwa", "PARSE EXCEPTION (in loadImageTask): "+e.getMessage());
		    		publishProgress(false);
		    	}
			  }
			});
		}
		
		@Override
		protected void onProgressUpdate(Boolean... params) {
			super.onProgressUpdate(params);
			if(!params[0]) {
				PinItUtils.createAlert("This is embarrassing", 
						"We couldn't load your notes this time, please try again", MainActivity.this);
			}
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if(mapMode == MapViewMode.YOUR_NOTES) { 
				LatLng southwest = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
				LatLng northeast = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
					
				LatLngTuple tuple = new LatLngTuple(southwest, northeast);
				LoadNotesTask currentUserNotesTask = new LoadNotesTask(tuple, true);
				currentUserNotesTask.execute();
			}
		}	
	}
  
  class LoadNotesTask extends AsyncTask<Void, Void, List<ParseObject>> {
	private LatLngTuple tuple;
	
	private String errorMessage;
	private final String PARSE_BOX_LATITUDE_ERROR = "Box latitude height below precision limit.";
	private boolean loadCurrentUserNotes;
	
	public LoadNotesTask(LatLngTuple tuple, boolean loadCurrentUserNotes) {
		this.tuple = tuple;
		this.loadCurrentUserNotes = loadCurrentUserNotes;
	}

	@Override
	protected List<ParseObject> doInBackground(Void... params) {
		try {
			ParseQuery query = new ParseQuery("Note");
			if(loadCurrentUserNotes) {
				query.whereEqualTo("creator", mCurrentUsername);
			}
			else {
				query.whereNotEqualTo("creator", mCurrentUsername);
			}
			LatLng southwest = tuple.getSouthwest();
			LatLng northeast = tuple.getNortheast();
			boolean isProximateToIDLine = isProximateToIDLine(southwest, northeast);
			if(northeast.longitude < southwest.longitude && !isProximateToIDLine) {
				query.whereWithinGeoBox("geopoint", 
						new ParseGeoPoint(southwest.latitude, southwest.longitude),
						new ParseGeoPoint(northeast.latitude, 179.9));
				query.setLimit(5);
				ParseQuery postIDLineQuery = new ParseQuery("Note");
				if(loadCurrentUserNotes) {
					postIDLineQuery.whereEqualTo("creator", mCurrentUsername);
				}
				else {
					postIDLineQuery.whereNotEqualTo("creator", mCurrentUsername);
				}
				postIDLineQuery.whereWithinGeoBox("geopoint", 
						new ParseGeoPoint(southwest.latitude, -179.9), 
						new ParseGeoPoint(northeast.latitude, northeast.longitude));
				postIDLineQuery.setLimit(5);
				List<ParseObject> preIDLineNotes = query.find();
				List<ParseObject> postIDLineNotes = postIDLineQuery.find();
				preIDLineNotes.addAll(postIDLineNotes);
				return preIDLineNotes;
			}
			else if(!isProximateToIDLine){
				query.whereWithinGeoBox("geopoint", 
						new ParseGeoPoint(southwest.latitude, southwest.longitude), 
						new ParseGeoPoint(northeast.latitude, northeast.longitude));
				query.setLimit(10);
				return query.find();
			}
			
			return new ArrayList<ParseObject>();
			
		} catch (ParseException e) {
			Log.e("vishwa", "PARSE EXCEPTION (in loadnotestask): "+e.getMessage());
			errorMessage = e.getMessage();
			return null;
		}
	}

	private boolean isProximateToIDLine(LatLng southwest, LatLng northeast) {
		if((180 - southwest.longitude) < 0.1 || (northeast.longitude + 180) < 0.1) {
			return true;
		}
		return false;
	}
	
	@Override
	protected void onPostExecute(List<ParseObject> noteList) {
		super.onPostExecute(noteList);
		
		if(noteList == null && !errorMessage.equals(PARSE_BOX_LATITUDE_ERROR)) {
			PinItUtils.createAlert("This is embarrassing", 
					"We couldn't load your notes please try again", MainActivity.this);
		}
		else {
			if(noteList != null) {
				for(ParseObject note: noteList) {
					final double latitude = note.getParseGeoPoint("geopoint").getLatitude();
					final double longitude = note.getParseGeoPoint("geopoint").getLongitude();
					String noteTitle = note.getString("title");
					String noteBody = note.getString("body");
					String noteCreator = note.getString("creator");
					boolean hasPhoto = note.getBoolean("hasPhoto");
					String date = note.getCreatedAt().toString();
					String[] arr = date.split("\\s");
					String noteCreatedAt = arr[1] + " " + arr[2] + ", " + arr[5];
					String noteCreatedAtFull = date;
					String noteImageThumbnailUrl;
					if(hasPhoto) {
						noteImageThumbnailUrl = note.getParseFile("notePhotoThumbnail").getUrl();
					}
					else {
						noteImageThumbnailUrl = new String();
					}
					ParseGeoPoint noteGeoPoint = note.getParseGeoPoint("geopoint");
					Note userNote = new Note(note.getObjectId(), noteCreator, noteTitle, noteBody, 
							noteGeoPoint, noteImageThumbnailUrl, noteCreatedAt, noteCreatedAtFull);
					createMarkerAtLocation(latitude, longitude, userNote);
				}
			}
		}
	}
	  
  }
  
  @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
		case REQUEST_CODE_CREATE_NOTE:
			if(resultCode != Activity.RESULT_OK) {
				return;
			}
		    
		    double latitude = Double.parseDouble(data.getStringExtra("geopoint").split(",")[0]);
		    double longitude = Double.parseDouble(data.getStringExtra("geopoint").split(",")[1]);
		    String noteTitle = data.getStringExtra("noteTitle");
		    String noteBody = data.getStringExtra("noteBody");
		    String noteImageThumbnailUrl = data.getStringExtra("noteImageThumbnailUrl");
		    String noteCreatedAt = data.getStringExtra("noteCreatedAt");
		    String noteCreatedAtFull = data.getStringExtra("noteCreatedAtFull");
		    String noteId = data.getStringExtra("noteId");
		    ParseGeoPoint noteGeoPoint = new ParseGeoPoint(latitude, longitude);
		    Note note = new Note(noteId, mCurrentUsername, noteTitle, noteBody, noteGeoPoint, 
		    		noteImageThumbnailUrl, noteCreatedAt, noteCreatedAtFull);
		    createMarkerAtLocation(latitude, longitude, note);
		}
	}
  
  private void createMarkerAtLocation(final double latitude, final double longitude, final Note note) {
	  if(mReverseNoteStore.get(note.getNoteId()) == null) {
      Bitmap balloonBackground = BitmapFactory.decodeResource(
    		  getResources(), R.drawable.balloon_background);
      Bitmap userPhoto = null;
	
      if(note.getNoteCreator() == mCurrentUsername) {
		  balloonBackground = Bitmap.createScaledBitmap(balloonBackground, 87, 94, false);
		  userPhoto = Bitmap.createScaledBitmap(mUserPhotoThumbnail, 75, 71, false);
		  
		  Canvas canvas = new Canvas(balloonBackground);
		  canvas.drawBitmap(balloonBackground, 0, 0, null);
		  canvas.drawBitmap(userPhoto, 6, 6, null);
      }
      else {
    	  if(mMemoryCache.get(note.getNoteCreator()) != null) {
    		  balloonBackground = Bitmap.createScaledBitmap(balloonBackground, 87, 94, false);
    		  userPhoto = Bitmap.createScaledBitmap(
    				  mMemoryCache.get(note.getNoteCreator()), 75, 71, false);
    		  
    		  Canvas canvas = new Canvas(balloonBackground);
    		  canvas.drawBitmap(balloonBackground, 0, 0, null);
    		  canvas.drawBitmap(userPhoto, 6, 6, null);
    		  
    		  addMarkerToMap(note, balloonBackground, latitude, longitude);
    	  }
    	  else {
			  ParseQuery query = ParseUser.getQuery();
			  query.whereEqualTo("username", note.getNoteCreator());
			  try {
				  ParseObject noteCreator = query.find().get(0);
				  if(noteCreator.getBoolean("isDefaultPhoto")) {
					  balloonBackground = Bitmap.createScaledBitmap(balloonBackground, 87, 94, false);
		    		  userPhoto = Bitmap.createScaledBitmap(
		    				  mMemoryCache.get("defaultPhoto"), 75, 71, false);
		    		  
		    		  Canvas canvas = new Canvas(balloonBackground);
		    		  canvas.drawBitmap(balloonBackground, 0, 0, null);
		    		  canvas.drawBitmap(userPhoto, 6, 6, null);
		    		  
		    		  addMarkerToMap(note, balloonBackground, latitude, longitude);
				  }
				  ParseFile userPhotoFile = noteCreator.getParseFile("profilePhotoThumbnail");
			      userPhotoFile.getDataInBackground(new GetDataCallback() {
							
				      @Override
				      public void done(byte[] data, ParseException e) {
				    	if(e == null) {
				    		Bitmap balloonBackground = BitmapFactory.decodeResource(
				    	    		  getResources(), R.drawable.balloon_background);
				    		
				    		Bitmap userPhoto = BitmapFactory.decodeByteArray(data, 0, data.length);
				    		mMemoryCache.put(note.getNoteCreator(), userPhoto);
				    		
				    		balloonBackground = Bitmap.createScaledBitmap(balloonBackground, 87, 94, false);
				    		userPhoto = Bitmap.createScaledBitmap(userPhoto, 75, 71, false);
				    		  
				    		Canvas canvas = new Canvas(balloonBackground);
				    		canvas.drawBitmap(balloonBackground, 0, 0, null);
				    		canvas.drawBitmap(userPhoto, 6, 6, null);
				    		
				    		addMarkerToMap(note, balloonBackground, latitude, longitude);
						}
				    	else {
				    		Log.e("vishwa", "PARSE EXCEPTION (in creatingmarker): "+e.getMessage());
				    	}
					  }
					});
			  }
			  catch(ParseException e) {
				  PinItUtils.createAlert("This is embarrassing", "Please log out and login again", this);
			  }
    		  }
    	  }
	  }
  }
  
  public void addMarkerToMap(Note note, Bitmap balloonBackground, double latitude, double longitude) {
	  
	  LatLng geopoint = new LatLng(latitude, longitude);
    
	  Marker newMarker = mMap.addMarker(new MarkerOptions()
              .position(geopoint)
    		  .draggable(false)
    		  .icon(BitmapDescriptorFactory.fromBitmap(balloonBackground)));
	  
	  if(mNoteStore.get(newMarker.getId()) == null) {
		  mNoteStore.put(newMarker.getId(), note);
		  mReverseNoteStore.put(note.getNoteId(), newMarker.getId());
		  if(mMarkerList.size() == 20) {
			  Marker marker = mMarkerList.get(0);
			  if(mFocusedMarker != null) {
		    	if(marker.getId() == mFocusedMarker.getId()) {
		    		marker = mMarkerList.remove(1);
		    	}
			  }
		      else {
		    	  Note removeNote = mNoteStore.get(marker.getId());
		    	  mNoteStore.remove(marker.getId());
		    	  mReverseNoteStore.remove(removeNote.getNoteId());
		    	  marker.remove();
		      }
		    }
	    	else {
	    		mMarkerList.add(newMarker);
	    	}
	    }
  }

  @Override
  public void onMapLongClick(LatLng point) {
		Intent intent = new Intent(this.getApplicationContext(), CreateNoteActivity.class);
		intent.putExtra("geopoint", point.latitude + "," + point.longitude);
		startActivityForResult(intent, REQUEST_CODE_CREATE_NOTE);
  }
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_action_bar, menu);
    mMenu = menu;
    
    if(!mHasInternet) {
		hideNonRefreshMenuItems();
    }
    else {
    	showNonRefreshMenuItems();
    }
    return true;
  }
	
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    	case R.id.action_search:
    		final MenuItem createNoteItem = mMenu.getItem(1);
    		createNoteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    		item.setOnActionExpandListener(new OnActionExpandListener() {
				
				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					return true;
				}
				
				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					createNoteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					return true;
				}
			});
    		break;
    	case R.id.action_create_note:
    		Intent intent = new Intent(this.getApplicationContext(), CreateNoteActivity.class);
    		LatLng mapCenter = mMap.getCameraPosition().target;
    		intent.putExtra("geopoint", mapCenter.latitude + "," + mapCenter.longitude);
    		startActivityForResult(intent, REQUEST_CODE_CREATE_NOTE);
    		break;
    	case R.id.action_logout:
    		ParseUser.logOut();
    		finish();
    		break;
    	case R.id.action_refresh:
    		if(!PinItUtils.isOnline(getApplicationContext())) {
    	    	PinItUtils.createAlert("Internet connection not found.", 
    	    			"Connect to the Internet and press the refresh button at the top", this);
    	    	mHasInternet = false;
    	    	hideNonRefreshMenuItems();
    	    	
    	    }
    	    else {	
    	    	mHasInternet = true;
    	    	showNonRefreshMenuItems();
    	    	loadMapWhenOnline();
    	    	
    	    	mMap.getUiSettings().setAllGesturesEnabled(true);
    	    	mMap.getUiSettings().setZoomControlsEnabled(true);
    	    	mMap.getUiSettings().setZoomGesturesEnabled(true);
    	    }
    	default:
            return super.onOptionsItemSelected(item);
    }
    
	return true;
  }
  
  public void hideNonRefreshMenuItems() {
	  for(int i = 0; i < mMenu.size(); i++) {
  		if(mMenu.getItem(i).getItemId() != R.id.action_refresh) {
  			mMenu.getItem(i).setVisible(false);
  		}
  		else {
  			mMenu.getItem(i).setVisible(true);
  		}
  	}
  }
  
  public void showNonRefreshMenuItems() {
	  for(int i = 0; i < mMenu.size(); i++) {
  		if(mMenu.getItem(i).getItemId() != R.id.action_refresh) {
  			mMenu.getItem(i).setVisible(true);
  		}
  		else {
  			mMenu.getItem(i).setVisible(false);
  		}
  	}
  }

	@Override
	public void onMarkerDrag(Marker marker) {
	}
	
	@Override
	public void onMarkerDragEnd(Marker marker) {
	}
	
	@Override
	public void onMarkerDragStart(Marker marker) {
	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		
		mMap.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			private TextView noteTitleTextView;
			private TextView noteBodyTextView;
			private ImageView notePhotoImageView;
			private TextView noteCreatedAtTextView;
			private View view;
			private LinearLayout layout;
			
			private Note note;
			
			@Override
			public View getInfoContents(Marker arg0) {
				return null;
			}
			
			class FetchImageTask extends AsyncTask<String, Void, Bitmap> {
			    @Override
			    protected Bitmap doInBackground(String... arg0) {
			    	Bitmap notePhoto = null;
			    	try {
			    		 if(arg0.equals(null) || arg0.equals(""))
			    		 {
					    	 notePhotoImageView.setVisibility(ImageView.GONE);
					    	 return null;
			    		 }
			    		 else
			    		 {
				    		 notePhoto = BitmapFactory.decodeStream((InputStream) new URL(arg0[0]).getContent());
			    		 }
					} catch (MalformedURLException e) {
						return null;
					} catch (IOException e) {
						return null;
					} 
			        return notePhoto;
			    }	
			}

			@Override
			public View getInfoWindow(Marker marker){
				
				mFocusedMarker = marker;
				
				note = mNoteStore.get(marker.getId());
				
				LayoutInflater inflater = (LayoutInflater) getApplicationContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.balloon_infowindow, null);
				
				noteTitleTextView = (TextView) view.findViewById(R.id.balloon_note_title);
				noteBodyTextView = (TextView) view.findViewById(R.id.balloon_note_body);
				notePhotoImageView = (ImageView) view.findViewById(R.id.balloon_note_image);
		        noteCreatedAtTextView = (TextView) view.findViewById(R.id.balloon_create_info);
		        layout = (LinearLayout) view.findViewById(R.id.balloon_image_layout);
		        
		        notePhotoImageView.setAdjustViewBounds(true);
		       
		        noteTitleTextView.setText(note.getNoteTitle());
		        
		        if(!note.getNoteBody().trim().isEmpty()) {
		        	noteBodyTextView.setVisibility(TextView.VISIBLE);
		        	noteBodyTextView.setText(note.getNoteBody());
		        }
		        else {
		        	noteBodyTextView.setVisibility(TextView.GONE);
		        }
		        
		        noteCreatedAtTextView.setText(
		        		"Created by "+note.getNoteCreator()+" on "+ note.getNoteCreatedAt());
		        
		        if(!note.getNoteImageThumbnailUrl().trim().isEmpty()) {
			        FetchImageTask fetchImageTask = new FetchImageTask();
			        try {
			        	String noteId = note.getNoteId();
			        	Bitmap result = mMemoryCache.get(noteId);
						if(result != null) {
							notePhotoImageView.setImageBitmap(result);
							notePhotoImageView.setVisibility(ImageView.VISIBLE);
							layout.setVisibility(LinearLayout.VISIBLE);
						}
						else {
							result = fetchImageTask.execute(note.getNoteImageThumbnailUrl()).get();
							notePhotoImageView.setImageBitmap(result);
							notePhotoImageView.setVisibility(ImageView.VISIBLE);
							layout.setVisibility(LinearLayout.VISIBLE);
							mMemoryCache.put(noteId, result);
						}
					} catch (InterruptedException e) {
						return null;
					} catch (ExecutionException e) {
						return null;
					}
		        }
		     
		        return view;
			}
			
		});
		
		marker.showInfoWindow();
	
		if(mNoteStore.get(marker.getId()).getNoteImageThumbnailUrl().isEmpty()) {
			mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 500, null);
		}
		else {
			LatLng markerGeoPoint = marker.getPosition();
			Point markerPoint = mMap.getProjection().toScreenLocation(markerGeoPoint);
			Point poinToMoveCameraTo = new Point(markerPoint.x, markerPoint.y - 150);
			LatLng newMarkerGeoPoint = mMap.getProjection().fromScreenLocation(poinToMoveCameraTo);
			mMap.animateCamera(CameraUpdateFactory.newLatLng(newMarkerGeoPoint), 500, null);
		}
		return true;
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	        mMemoryCache.put(key, bitmap);
	    }
	}

	public Bitmap getBitmapFromMemCache(String key) {
	    return mMemoryCache.get(key);
	}

	@Override
	public void onBackPressed() {
		if(mFocusedMarker != null) {
			mFocusedMarker.hideInfoWindow();
			mFocusedMarker = null;
		}
		else {
			super.onBackPressed();
		}
	}
	
	
} 
