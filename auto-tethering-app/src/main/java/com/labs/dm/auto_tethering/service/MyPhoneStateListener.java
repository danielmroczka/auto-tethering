package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;

import static com.labs.dm.auto_tethering.TetherIntents.CHANGE_CELL;

/**
 * Created by Daniel Mroczka on 20-Dec-16.
 */

public class MyPhoneStateListener extends PhoneStateListener {

    private Context context;

    public MyPhoneStateListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {
        super.onCellLocationChanged(location);
        context.sendBroadcast(new Intent(CHANGE_CELL));
    }

}