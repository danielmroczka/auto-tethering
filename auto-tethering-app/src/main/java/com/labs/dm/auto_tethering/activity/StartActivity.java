package com.labs.dm.auto_tethering.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends Activity {

    static final int MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 100;
    static final int MY_PERMISSIONS_REQUEST = 70;

    private boolean hasWritePermission = false;
    private boolean hasLocationPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWritePermission();
        setLocationsPermission();
        check();
    }

    private void setWritePermission() {
        hasWritePermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                hasWritePermission = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
            }
        }

    }

    private boolean hasNotPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED;
    }

    private void setLocationsPermission() {
        //if (ActivityCompat.shouldShowRequestPermissionRationale(this,

        hasLocationPermission = true;
        List<String> perms = new ArrayList<>();
        if (hasNotPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (hasNotPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (hasNotPermission(Manifest.permission.READ_PHONE_STATE)) {
            perms.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!perms.isEmpty()) {
            hasLocationPermission = false;
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), MY_PERMISSIONS_REQUEST);
        } else {
//            check();
        }

    }

    private void check() {
        if (hasWritePermission && hasLocationPermission) {
            onPermissionsOkay();
        } else {
            if (!hasWritePermission) {
                setWritePermission();
            }

            if (!hasLocationPermission) {
                setLocationsPermission();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case MY_PERMISSIONS_MANAGE_WRITE_SETTINGS: {
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    hasWritePermission = true;
                    if (!hasLocationPermission)
                        setLocationsPermission();
                } else {
                    setWritePermission();
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST: {
                if (resultCode == RESULT_OK) {
                    hasLocationPermission = true;
                    if (!hasWritePermission) setWritePermission();
                } else {
                    setLocationsPermission();
                }
                break;
            }

        }

        check();
    }

    private void onPermissionsOkay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(StartActivity.this, MainActivity.class));
                finish();
            }
        }, 1);
    }

}
