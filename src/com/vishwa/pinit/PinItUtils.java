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

import java.io.IOException;
import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;

public class PinItUtils {
    
    private final static String SHARED_PREFERENCES_FILENAME = "PinItSharedPreferences";

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static void createAlert(String errorTitle, String errorMessage, Context ctx)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle(errorTitle);
        dialog.setMessage(errorMessage);
        dialog.setPositiveButton("Close", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface arg0, int arg1) {
                return;
            }
        });
        dialog.show();
    }

    public static String getAbsolutePathFromUri(Context ctx, Uri contentUri) {
        String[] proj = { MediaStore.Video.Media.DATA };
        Cursor cursor = ctx.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public static Matrix getRotationMatrixForImage(Context ctx, Uri contentUri)  {

        String pathToImage = getAbsolutePathFromUri(ctx, contentUri);
        ExifInterface exif;
        try {
            exif = new ExifInterface(pathToImage);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Matrix matrix = new Matrix();
            if(orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            }
            else if(orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            }
            else if(orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }
            return matrix;
        } catch (IOException e) {
            return new Matrix();
        }
    }
    
    public static Bitmap decodeSampledBitmapFromFilePath(String filePath, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
    
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
    
        if (height > reqHeight || width > reqWidth) {
    
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
    
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            inSampleSize = inSampleSize < 2 ? 2: inSampleSize;
        }
    
        return inSampleSize;
    }
    
    public static String getFormattedCommentCreatedAt(String date) {
        String[] arr = date.split("\\s");
        Calendar calendar = Calendar.getInstance();
        StringBuilder stringBuilder = new StringBuilder();
        String hours = arr[3].split(":")[0];
        String minutes = arr[3].split(":")[1];
        if(Integer.parseInt(arr[5]) != calendar.get(Calendar.YEAR)) {
            stringBuilder.append(arr[1])
                .append(' ')
                .append(arr[2])
                .append(',')
                .append(' ')
                .append(arr[5]);
        }
        else {
            stringBuilder.append(arr[1])
                .append(' ')
                .append(arr[2])
                .append(" at ");
        }
        if(Integer.parseInt(hours) < 12) {
            stringBuilder.append(" at ")
            .append(hours)
            .append(':')
            .append(minutes);
            stringBuilder.append(" AM ");
        }
        else {
            int hoursDigits = Integer.parseInt(hours);
            hoursDigits = hoursDigits - 12;
            hours = Integer.toString(hoursDigits);
            stringBuilder.append(" at ")
            .append(hours)
            .append(':')
            .append(minutes);
            stringBuilder.append(" PM ");
        }
        return stringBuilder.toString();
    }
    
    public static boolean isUsersFirstLogin(String username, Context context) {
        SharedPreferences sharedPreferences = 
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        if(sharedPreferences.getBoolean(username+"_first_login_flag", true)) {
            return true;
        }
        return false;
    }
    
    public static void finishUsersFirstLogin(String username, Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        sharedPreferences.edit().putBoolean(username+"_first_login_flag", false).commit();
    }
    
    public static boolean isUsersFirstMarkerClick(String username, Context context) {
        SharedPreferences sharedPreferences = 
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        if(sharedPreferences.getBoolean(username+"_first_markerclick_flag", true)) {
            return true;
        }
        return false;
    }
    
    public static void finishUsersFirstMarkerClick(String username, Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        sharedPreferences.edit().putBoolean(username+"_first_markerclick_flag", false).commit();
    }
    
    public static boolean isUsersFirstNoteCreate(String username, Context context) {
        SharedPreferences sharedPreferences = 
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        if(sharedPreferences.getBoolean(username+"_first_notecreate_flag", true)) {
            return true;
        }
        return false;
    }
    
    public static void finishUsersFirstNoteCreate(String username, Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        sharedPreferences.edit().putBoolean(username+"_first_notecreate_flag", false).commit();
    }

}
