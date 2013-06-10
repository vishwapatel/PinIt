package com.vishwa.pinit;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;

public class PinItUtils {

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

}
