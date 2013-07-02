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

import java.lang.reflect.Field;

import android.app.Application;
import android.view.ViewConfiguration;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

public class PinItApplication extends Application {
    
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    
    private boolean hasUserLoggedInSuccesfully = false;
    
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, Constants.PARSE_APP_ID, Constants.PARSE_CLIENT_KEY); 
        ParseFacebookUtils.initialize(Constants.FACEBOOK_APP_ID);
        
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 10; 
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mImageLoader = new ImageLoader(mRequestQueue, new BitmapLruCache(cacheSize));

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
        }
    }
    
    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
    
    public boolean getHasUserLoggedInSuccessfully() {
        return hasUserLoggedInSuccesfully;
    }
    
    public void setHasUserLoggedInSuccesfully(boolean hasUserLoggedInSuccessfully) {
        this.hasUserLoggedInSuccesfully = hasUserLoggedInSuccessfully;
    }

}