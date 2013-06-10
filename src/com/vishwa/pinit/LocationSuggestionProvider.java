package com.vishwa.pinit;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.provider.BaseColumns;
import android.util.Log;

public class LocationSuggestionProvider extends ContentProvider {

    private Geocoder mGeocoder;

    private static final String LOG_TAG = "vishwa";

    public static final String AUTHORITY = "com.vishwa.pinit.search_suggestion_provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/search");

    private static final int SEARCH_SUGGEST = 1;

    private static final AndroidHttpClient mAndroidHttpClient = 
            AndroidHttpClient.newInstance(LocationSuggestionProvider.class.getName());

    private static final UriMatcher uriMatcher;

    private static final String[] SEARCH_SUGGEST_COLUMNS = {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
    };

    private String mGoogleGeocodingEndpoint = "http://maps.googleapis.com/maps/api/geocode/json?address=";

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.d(LOG_TAG, "query = " + uri);

        // Use the UriMatcher to see what kind of query we have
        switch (uriMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            String query = uri.getLastPathSegment();

            MatrixCursor cursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS, 1);
            if(Geocoder.isPresent()) {
                try {
                    mGeocoder = new Geocoder(getContext(), Locale.ENGLISH);
                    List<Address> addressList = mGeocoder.getFromLocationName(query, 5);
                    for(int i = 0; i < addressList.size(); i++) {
                        Address address = addressList.get(i);
                        StringBuilder fullAddress = new StringBuilder();
                        for(int j = 1; j < address.getMaxAddressLineIndex(); j++) {
                            fullAddress.append(address.getAddressLine(j)); 
                        }
                        cursor.addRow(new String[] {Integer.toString(i), 
                                address.getAddressLine(0).toString(), 
                                fullAddress.toString(), 
                                address.getLatitude()+","+address.getLongitude()});
                    }

                    return cursor;
                }
                catch (IllegalArgumentException e) {
                    return getLocationSuggestionsUsingAPI(query, cursor);
                }
                catch (IOException e) {
                    return getLocationSuggestionsUsingAPI(query, cursor);
                }    
            }
        default:
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    private MatrixCursor getLocationSuggestionsUsingAPI(String query, MatrixCursor cursor){
        String transformedQuery = query.replace(" ", "+");
        String queryUrl = mGoogleGeocodingEndpoint + transformedQuery + "&sensor=false";

        try {

            JSONObject apiResponse = new JSONObject(mAndroidHttpClient.execute(new HttpGet(queryUrl),
                    new BasicResponseHandler()));
            JSONArray results = (JSONArray) apiResponse.get("results");

            for(int i = 0; i < results.length(); i++) {
                String formattedAddress;
                JSONObject result = results.getJSONObject(i);
                if(result.has("formatted_address")) {
                    formattedAddress = result.getString("formatted_address");
                    if(result.has("geometry")) {
                        JSONObject geometry = result.getJSONObject("geometry");
                        if(geometry.has("location")) {
                            JSONObject location = geometry.getJSONObject("location");
                            double latitude = location.getDouble("lat");
                            double longitude = location.getDouble("lng");
                            cursor.addRow(new String[] {Integer.toString(i), 
                                    formattedAddress, 
                                    "", 
                                    latitude+","+longitude});
                        }
                    }
                }
            }
            return cursor;
        } catch (ClientProtocolException e) {
            cursor.addRow(new String[] {"0", "Search is not available currently", "Try again later.", ""});
            return cursor;
        } catch (JSONException e) {
            cursor.addRow(new String[] {"0", "Search is not available currently", "Try again later.", ""});
            return cursor;
        } catch (IOException e) {
            cursor.addRow(new String[] {"0", "Search is not available currently", "Try again later.", ""});
            return cursor;
        }
    }

    @Override
    public int update(Uri uri, ContentValues arg1, String arg2, String[] arg3) {
        throw new UnsupportedOperationException();
    }
}