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


import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.andreabaccega.widget.FormEditText;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;


public class LoginActivity extends Activity {

    private static final int USERNAME_TAKEN = 0;
    private static final int USERNAME_MISSING = 0;

    private Button mLoginButton;
    private Button mSignupButton;
    private Button mFbLoginButton;
    private Button mForgotPasswordButton;
    private FormEditText mUsernameField;
    private FormEditText mPasswordField;
    private EditText mPasswordResetField;

    private Bitmap mUserPhoto = null;

    private ParseUser mCurrentUser;

    private byte[] mPhotoByteArray = null;
    private boolean hasUserLoggedInSuccessfully = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            hasUserLoggedInSuccessfully = true;
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_login);

        setProgressBarIndeterminateVisibility(false);

        mLoginButton = (Button) findViewById(R.id.login_button);
        mSignupButton = (Button) findViewById(R.id.login_signup_button);
        mFbLoginButton = (Button) findViewById(R.id.login_fb_button);
        mForgotPasswordButton = (Button) findViewById(R.id.login_forgot_password);
        mUsernameField = (FormEditText) findViewById(R.id.login_username_field);
        mPasswordField = (FormEditText) findViewById(R.id.login_password_field);

        resizeButtons();

        mLoginButton.setOnClickListener(new OnLoginClickListener());
        mFbLoginButton.setOnClickListener(new OnFbLoginClickListener());
        mSignupButton.setOnClickListener(new OnSignupClickListener());
        mForgotPasswordButton.setOnClickListener(new OnForgotPasswordClickListener());
    }

    public class OnForgotPasswordClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            final AlertDialog dialog = createForgotPasswordDialog();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    final String email = mPasswordResetField.getText().toString();
                    if(email.isEmpty()) {
                        mPasswordResetField.setError("Enter an email");
                    }
                    else {
                        mPasswordResetField.setError(null);
                        setProgressBarIndeterminateVisibility(true);
                        ParseUser.requestPasswordResetInBackground(email, new RequestPasswordResetCallback() {

                            @Override
                            public void done(ParseException e) {
                                setProgressBarIndeterminateVisibility(false);
                                if(e == null) {
                                    Toast.makeText(getApplicationContext(), "An email has been sent to "+email+
                                            " with a link to reset your password. Please check it.", Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                                else if(e.getCode() == ParseException.EMAIL_NOT_FOUND) {
                                    PinItUtils.createAlert("Email not found", "Sorry, that email address " +
                                            "doesn't exist, please create a new account with it.", LoginActivity.this);
                                }
                                else {
                                    PinItUtils.createAlert("We're sorry", "That email is invalid", LoginActivity.this);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    class OnLoginClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(PinItUtils.isOnline(getApplicationContext())) {
                FormEditText[] allFields = { mUsernameField, mPasswordField };

                boolean allValid = true;
                for(FormEditText field: allFields) {
                    allValid = field.testValidity() && allValid;
                }

                if(allValid) {
                    String username = mUsernameField.getText().toString().trim();
                    String password = mPasswordField.getText().toString();

                    setProgressBarIndeterminateVisibility(true);

                    ParseUser.logInInBackground(username, password, new LogInCallback() {

                        @Override
                        public void done(ParseUser user, ParseException e) {

                            setProgressBarIndeterminateVisibility(false);

                            if (e == null) {
                                hasUserLoggedInSuccessfully = true;
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else {
                                String error = e.getMessage().substring(0, 1).toUpperCase() + 
                                        e.getMessage().substring(1);

                                PinItUtils.createAlert("Unable to login", error, LoginActivity.this);
                            }
                        }
                    });
                }
            }
            else {
                PinItUtils.createAlert("Internet connection not found.", "This app "+
                        "needs an active Internet connection!", LoginActivity.this);
            }
        }

    }

    class OnFbLoginClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(PinItUtils.isOnline(getApplicationContext())) {
                setProgressBarIndeterminateVisibility(true);
                ParseFacebookUtils.logIn(LoginActivity.this, new LogInCallback() {

                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if(e == null) {
                            if(user != null) {                    
                                if(user.isNew()) {
                                    mCurrentUser = user;
                                    setupParseAccountInBackground();
                                }
                                else {
                                    if(ParseUser.getCurrentUser().getParseFile("profilePhotoThumbnail") != null) {
                                        hasUserLoggedInSuccessfully = true;
                                        setProgressBarIndeterminateVisibility(false);
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else {
                                        setProgressBarIndeterminateVisibility(true);
                                        mCurrentUser = user;
                                        setupParseAccountInBackground();
                                    }
                                }
                            }
                        }    
                        else {
                            String error = e.getMessage().substring(0, 1).toUpperCase() + 
                                    e.getMessage().substring(1);

                            PinItUtils.createAlert("Unable to login", error, LoginActivity.this);
                        }
                    }

                });
            }
            else {
                PinItUtils.createAlert("Internet connection not found.", "This app "+
                        "needs an active Internet connection!", LoginActivity.this);
            }
        }

    }

    private void setupParseAccountInBackground() {
        Request.executeMeRequestAsync(ParseFacebookUtils.getSession(), new Request.GraphUserCallback() {

            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
                    try {
                        String userPhotoUrl = String.format(
                                "http://graph.facebook.com/%s/picture?width=100&height=100", user.getId());
                        mUserPhoto = new DownloadImageFromUrlTask(
                                getApplicationContext(), user.getUsername()).execute(userPhotoUrl).get();
                        if(mUserPhoto != null) {
                            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                            mUserPhoto.compress(CompressFormat.PNG, 100, byteStream);
                            mPhotoByteArray = byteStream.toByteArray();

                            recycleAllBitmaps();

                            mCurrentUser.put("isDefaultPhoto", false);
                            mCurrentUser.put("fbId", user.getId());
                            if(user.getUsername() != null) {
                                if(user.getUsername().isEmpty()) {
                                    setProgressBarIndeterminateVisibility(false);
                                    String errorMessage = "It seems like you don't have a Facebook " +
                                            "username, please enter a username here:";
                                    createUsernameErrorDialog(errorMessage, USERNAME_MISSING).show();  
                                }
                                else {
                                    mCurrentUser.setUsername(user.getUsername());
                                    final ParseFile userPhotoThumbnailFile = 
                                            new ParseFile("photoThumbnail.png", mPhotoByteArray);
                                    userPhotoThumbnailFile.saveInBackground(new SaveCallback() {

                                        @Override
                                        public void done(ParseException e) {
                                            if(e == null) {
                                                mCurrentUser.put("profilePhotoThumbnail", userPhotoThumbnailFile);
                                                saveParseUserInBackground(mCurrentUser);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void saveParseUserInBackground(ParseUser user) {
        user.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                setProgressBarIndeterminateVisibility(false);
                if(e == null) {
                    hasUserLoggedInSuccessfully = true;
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if(e.getCode() == ParseException.USERNAME_TAKEN) {
                    hasUserLoggedInSuccessfully = false;
                    String errorMessage = "Your Facebook username has already been taken, please enter "+
                            "another username:";
                    createUsernameErrorDialog(errorMessage, USERNAME_TAKEN).show();
                }
                else {
                    hasUserLoggedInSuccessfully = false;
                    String error = e.getMessage().substring(0, 1).toUpperCase()+ 
                            e.getMessage().substring(1);
                    PinItUtils.createAlert("Login in using Facebook failed", error, LoginActivity.this);
                }
            }
        });
    }

    class OnSignupClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent signupIntent = new Intent(getApplicationContext(),
                    SignupActivity.class);
            startActivity(signupIntent);
        }
    }

    private AlertDialog createForgotPasswordDialog() {
        String title = "Password Reset";
        String message = "Enter the email you had signed up with to reset your password. You will " +
                "receive an email to reset your password on completion.";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        mPasswordResetField = new EditText(this);
        builder.setView(mPasswordResetField);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, int which) {
                //We are overriding this to create the button with the Ok label. Android
                //automatically dismisses AlertDialogs on button clicks, so to avoid this, we have to
                //override the button's on click listener separately (done in the
                //OnForgotPasswordClickListener class's onClick method).
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setProgressBarIndeterminateVisibility(false);
                return;
            }
        });

        return builder.create();
    }

    private Dialog createUsernameErrorDialog(String errorMessage, final int errorCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("We're sorry");
        builder.setMessage(errorMessage);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String username = input.getText().toString();
                if(username.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Username cannot be empty", Toast.LENGTH_LONG).show();
                    return;
                }
                mCurrentUser.setUsername(username);
                if(errorCode == USERNAME_MISSING) {
                    setProgressBarIndeterminateVisibility(true);
                    final ParseFile userPhotoThumbnailFile = 
                            new ParseFile("photoThumbnail.png", mPhotoByteArray);
                    userPhotoThumbnailFile.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                mCurrentUser.put("profilePhotoThumbnail", userPhotoThumbnailFile);
                                saveParseUserInBackground(mCurrentUser);
                            }
                        }
                    }); 
                }
                else {
                    setProgressBarIndeterminateVisibility(true);
                    saveParseUserInBackground(mCurrentUser);
                }
                return;
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setProgressBarIndeterminateVisibility(false);
                return;
            }
        });

        return builder.create();
    }

    public void resizeButtons() {
        ViewGroup.LayoutParams signupParams = mSignupButton.getLayoutParams();
        ViewGroup.LayoutParams loginParams = mLoginButton.getLayoutParams();
        ViewGroup.LayoutParams fbLoginParams = mFbLoginButton.getLayoutParams();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        signupParams.width = (int) width / 2;
        loginParams.width = (int) ((width*3)/5);
        fbLoginParams.width= (int) ((width*3)/5);

        mSignupButton.setLayoutParams(signupParams);
        mLoginButton.setLayoutParams(loginParams);
        mFbLoginButton.setLayoutParams(fbLoginParams);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
    }

    public void recycleAllBitmaps() {
        if(mUserPhoto != null) {
            mUserPhoto.recycle();
            mUserPhoto = null;
        }
    }

    @Override
    protected void onDestroy() {
        recycleAllBitmaps();
        if(ParseUser.getCurrentUser() != null && !hasUserLoggedInSuccessfully) {
            try {
                ParseUser.getCurrentUser().delete();
                ParseUser.logOut();
            } catch (ParseException e) {

            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}

