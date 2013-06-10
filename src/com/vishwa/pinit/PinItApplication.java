package com.vishwa.pinit;

import com.parse.Parse;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class PinItApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, Constants.PARSE_APP_ID, Constants.PARSE_CLIENT_KEY); 

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

}