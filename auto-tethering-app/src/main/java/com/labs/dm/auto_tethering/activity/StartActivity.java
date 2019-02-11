package com.labs.dm.auto_tethering.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class StartActivity extends Activity {

    static final int MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 100;
    static final int MY_PERMISSIONS_REQUEST = 70;

    private boolean hasWritePermission = false;
    private boolean hasLocationPermission = true;
    private boolean infoDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        setWritePermission();
    }

    /**
     * Sets permission to write to enable tethering feature
     */
    private void setWritePermission() {
        hasWritePermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                if (!infoDisplayed) {
                    LayoutInflater li = LayoutInflater.from(this);
                    final View promptsView = li.inflate(R.layout.permission_dialog, null);
                    AlertDialog dlg = new AlertDialog.Builder(this).create();
                    dlg.setTitle("Write permission request");
                    dlg.setView(promptsView);
                    dlg.setButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            hasWritePermission = false;
                            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
                        }
                    });

                    dlg.show();
                    infoDisplayed = true;
                } else {
                    hasWritePermission = false;
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
                }
            } else {
                setLocationsPermission();
            }
        } else {
            setLocationsPermission();
        }
    }

    private void setLocationsPermission() {
        String[] permissionsToGrant = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE};
        hasLocationPermission = true;
        List<String> perms = new ArrayList<>();

        for (String permission : permissionsToGrant) {
            if (hasNotPermission(permission)) {
                perms.add(permission);
            }
        }

        if (!perms.isEmpty()) {
            hasLocationPermission = false;
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), MY_PERMISSIONS_REQUEST);
        } else {
            check();
        }
    }

    private void check() {
        if (hasWritePermission && hasLocationPermission) {
            onGrantedPermissions();
            return;
        }

        new AlertDialog.Builder(StartActivity.this)
                .setTitle(R.string.warning)
                .setMessage("To properly running application you have to grant all required permission\nDo you want to grant them again otherwise application will be closed?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setWritePermission();
                    }
                })
                .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            hasLocationPermission = true;
            for (int perm : grantResults) {
                if (perm == PackageManager.PERMISSION_DENIED) {
                    hasLocationPermission = false;
                    break;
                }
            }
            check();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_MANAGE_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(getApplicationContext())) {
                hasWritePermission = true;
            }
            setLocationsPermission();
        }
    }

    private boolean hasNotPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED;
    }

    private void onGrantedPermissions() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(StartActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    public void onClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.labs.dm.auto_tethering"));
        startActivity(browserIntent);
    }

}
