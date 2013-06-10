package com.vishwa.pinit;

import android.app.Activity;
import android.graphics.Typeface;
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
			"there. I'm a junior studying computer science at the University of Pennsylvania. I " +
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
			"Feel free to reach out to me with feedback at vishwa@seas.upenn.edu";
	
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
