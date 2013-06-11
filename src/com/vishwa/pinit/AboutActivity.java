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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class AboutActivity extends Activity {

    private TextView mAboutMeTextView;
    private TextView mGoogleAttributionTextView;
    private Button mCloseButton;

    private String mAboutMe = "PinIt is developed and maintained by a swell fellow called Vishwa Patel. Yep, that's me up " +
            "there. I'm a junior at the University of Pennsylvania studying computer science and theatre. I " +
            "enjoy developing beautiful apps and paying close attention to building a great user " +
            "experience. I also believe in open and free software. This is why PinIt is free and " +
            "completely open-source and you can find it's source code at www.github.com/vishwapatel/pinit \n" +
            "You can find me at www.vishwa.me \n \n" +
            "PinIt was inspired by Findery (www.findery.com) a great web application that allows " +
            "users to discover new places and create great memories. I love their web-app and " +
            "wanted to build a beautiful Android app that was entirely built using native " +
            "components and that lets users discover the magic of visiting a new place and " +
            "learning some quirky aspect of that place, or discovering great places to eat in an " +
            "area and the amazing food they serve or discovering where someone was born and grew up." +
            " These are all beautiful experiences and I wanted to capture them through PinIt. I've " +
            "loved building it and will continue to do so. I hope you love using it too! \n" +
            "Feel free to reach out to me with feedback at vishwa@seas.upenn.edu \n" +
            "Please note that the source code is released under Apache License 2.0, a copy of which is" +
            "available in the assets directory of the source code and is also available at www.apache.org/licenses/LICENSE-2.0.html";

    private String mGoogleLicenseInfo = "Google Play Services are not available.";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_about);

        mAboutMeTextView = (TextView) findViewById(R.id.about_about_me);
        mGoogleAttributionTextView = (TextView) findViewById(R.id.about_google_attribution);
        mCloseButton = (Button) findViewById(R.id.about_close_button);

        mAboutMeTextView.setText(mAboutMe);

        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()) == ConnectionResult.SUCCESS) {
            mGoogleLicenseInfo = 
                    GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext());
        }

        mGoogleAttributionTextView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PinItUtils.createAlert(
                        "Google's Terms and Conditions", mGoogleLicenseInfo, AboutActivity.this);
            }
        });

        mCloseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

}
