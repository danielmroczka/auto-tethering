package com.labs.dm.auto_tethering.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public abstract class PermissionsActivity extends PreferenceActivity {

    static final int MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 100;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 69;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 70;
    static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 71;

    private boolean mLocationPermission = false;
    private boolean mSettingPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingPermission();
        /**
         * Locations permission done in onActivityResult
         */
        locationsPermission();

        //if (mLocationPermission && mSettingPermission) onPermissionsOkay();
    }


    private void settingPermission() {
        mSettingPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                mSettingPermission = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
            }
        }
    }

    private void locationsPermission() {
        mLocationPermission = true;

        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        }
//
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            mLocationPermission = false;
//            // Permission is not granted
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
//
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {
//
//                // No explanation needed; request the permission
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_PHONE_STATE},
//                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
//
//                // MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION is an
//                // app-defined int constant. The callback method gets the
//                // result of the request.
//            }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == MY_PERMISSIONS_MANAGE_WRITE_SETTINGS) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mSettingPermission = true;
                if (!mLocationPermission) locationsPermission();
            } else {
                settingPermission();
            }
        }

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION || requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION || requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mLocationPermission = true;
                if (!mSettingPermission) settingPermission();
            } else {
                locationsPermission();
            }
        }

        // if (mLocationPermission && mSettingPermission) onPermissionsOkay();

    }


    //abstract void onPermissionsOkay();


}
