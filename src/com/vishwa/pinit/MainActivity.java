package com.vishwa.pinit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

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
import com.google.common.collect.HashBiMap;
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
  
  private Bitmap mUserPhotoThumbnail;
  private LruCache<String, Bitmap> mMemoryCache;
  
  private String mCurrentUsername;
  private ArrayDeque<Marker> mMarkerList = new ArrayDeque<Marker>(20);
  private HashMap<String, Note> mNoteStore = new HashMap<String, Note>();
  private HashBiMap<String, Marker> mMarkerStore = HashBiMap.create(20);
  private boolean mHasInternet = true;

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
    	    	
//    	    	for(Marker marker: mMarkerList) {
//    	    		mNoteStore.remove(marker.getId());
//    	    		marker.remove();
//    	    	}
//    	    	mMarkerList.clear();
//    	    	mNoteStore.clear();
		    }
			else {
				mHasInternet = true;
				LatLngBounds mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;	
				LatLng southwest = mapBounds.southwest;
				LatLng northeast = mapBounds.northeast;
				
				LatLngTuple tuple = new LatLngTuple(southwest, northeast);
				LoadCurrentUserNotesTask currentUserNotesTask = new LoadCurrentUserNotesTask(tuple);
				currentUserNotesTask.execute();
				
//				Iterator<Entry<String, Marker>> iterator = mMarkerStore.entrySet().iterator();
//				while(iterator.hasNext()) {
//					Entry<String, Marker> entry = iterator.next();
//					Marker marker = entry.getValue();
//					if(!mapBounds.contains(marker.getPosition())) {
//						iterator.remove();
//						mNoteStore.remove(entry.getKey());
//						marker.remove();
//					}
//				}
//				Iterator<Marker> markerIterator = mMarkerList.iterator();
//				while(markerIterator.hasNext()) {
//					Marker marker = markerIterator.next();
//					if(!mapBounds.contains(marker.getPosition())) {
//						mNoteStore.remove(marker.getId());
//						marker.remove();
//						markerIterator.remove();
//					}
//				}
				
				Log.d("vishwa", "Size: " + mMarkerStore.size());
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
			
			LatLng southwest = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
			LatLng northeast = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
				
			LatLngTuple tuple = new LatLngTuple(southwest, northeast);
			LoadCurrentUserNotesTask currentUserNotesTask = new LoadCurrentUserNotesTask(tuple);
			currentUserNotesTask.execute();
		}	
	}
  
  class LoadCurrentUserNotesTask extends AsyncTask<Void, Void, List<ParseObject>> {
	private LatLngTuple tuple;
	
	private String errorMessage;
	private final String PARSE_BOX_LATITUDE_ERROR = "Box latitude height below precision limit.";
	
	public LoadCurrentUserNotesTask(LatLngTuple tuple) {
		this.tuple = tuple;
	}

	@Override
	protected List<ParseObject> doInBackground(Void... params) {
		try {
			ParseQuery query = new ParseQuery("Note");
			query.whereEqualTo("creator", mCurrentUsername);
			LatLng southwest = tuple.getSouthwest();
			LatLng northeast = tuple.getNortheast();
			boolean isProximateToIDLine = isProximateToIDLine(southwest, northeast);
			if(northeast.longitude < southwest.longitude && !isProximateToIDLine) {
				query.whereWithinGeoBox("geopoint", 
										new ParseGeoPoint(southwest.latitude, southwest.longitude),
										new ParseGeoPoint(northeast.latitude, 179.9));
				query.setLimit(5);
				ParseQuery postIDLineQuery = new ParseQuery("Note");
				postIDLineQuery.whereEqualTo("creator", mCurrentUsername);
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
  
  private void createMarkerAtLocation(double latitude, double longitude, Note note) {
		Bitmap balloonBackground = BitmapFactory.decodeResource(
				getResources(), R.drawable.balloon_background);
		
		balloonBackground = Bitmap.createScaledBitmap(balloonBackground, 87, 94, false);
		mUserPhotoThumbnail = Bitmap.createScaledBitmap(mUserPhotoThumbnail, 75, 71, false);

	    Canvas canvas = new Canvas(balloonBackground);
	    canvas.drawBitmap(balloonBackground, 0, 0, null);
	    canvas.drawBitmap(mUserPhotoThumbnail, 6, 6, null);

	    LatLng geopoint = new LatLng(latitude, longitude);
	    
	    Marker newMarker = mMap.addMarker(new MarkerOptions()
	    		.position(geopoint)
	    		.draggable(false)
	    		.icon(BitmapDescriptorFactory.fromBitmap(balloonBackground)));
	    
	    if(mMarkerStore.get(note.getNoteId()) == null) {
	    	mMarkerStore.put(note.getNoteId(), newMarker);
	    	mNoteStore.put(note.getNoteId(), note);
	    }
	    if(mNoteStore.get(note.getNoteId()) != null) {
	    	mNoteStore.put(note.getNoteId(), note);
	    }
//	    if(mNoteStore.get(newMarker.getId()) == null) {
//	    	mNoteStore.put(newMarker.getId(), note);
//	    }
	    
//	    if(mMarkerList.size() == 20) {
//	    	Marker removedMarker = mMarkerList.removeFirst();
//	    	mNoteStore.remove(removedMarker.getId());
//	    	removedMarker.remove();
//	    	mMarkerList.addLast(newMarker);
//	    }
//	    else {
//	    	mMarkerList.addLast(newMarker);
//	    }
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
    SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
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
	
	//The following code looks pretty ugly and doesn't follow the code conventions I've used
	// in the rest of the project, but it was just a trial to get a basic version ready.
	@Override
	public boolean onMarkerClick(Marker marker) {
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
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} 
			        return notePhoto;
			    }	
			}

			@Override
			public View getInfoWindow(Marker marker){
//				note = mNoteStore.get(marker.getId());
				note = mNoteStore.get(mMarkerStore.inverse().get(marker));
				
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
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
		        }
		        
		        return view;
			}
			
		});
		
		marker.showInfoWindow();
		
//		mMap.animateCamera(CameraUpdateFactory.)
//		Note note = mNoteStore.get(marker.getId());
//		Intent intent = new Intent(getApplicationContext(), DisplayNoteActivity.class);
//		intent.putExtra("noteTitle", note.getNoteTitle());
//		intent.putExtra("noteBody", note.getNoteBody());
//		intent.putExtra("noteCreator", note.getNoteCreator());
//		intent.putExtra("noteGeoPoint", note.getNoteGeoPoint().getLatitude() + "," + note.getNoteGeoPoint().getLongitude());
//		startActivityForResult(intent, REQUEST_CODE_DISPLAY_NOTE);
		return false;
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	        mMemoryCache.put(key, bitmap);
	    }
	}

	public Bitmap getBitmapFromMemCache(String key) {
	    return mMemoryCache.get(key);
	}
} 
