package com.labs.dm.auto_tethering;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MainInstrumentalTest {

    private final Context context = InstrumentationRegistry.getContext();
    private final ServiceHelper helper = new ServiceHelper(context);

    @Test
    public void getCellInfo() {
        Cellular cellular = Utils.getCellInfo(context);
        assertTrue(cellular.isValid());
    }

    @Test
    public void getBestLocation() {
        Location location = Utils.getBestLocation(context);
        assertTrue(location.getAccuracy() > 0F);
    }

    @Test
    public void getBluetoothDevices() {
        List<BluetoothDevice> devices = Utils.getBluetoothDevices(context, true);
        assertNotNull(devices);
    }

    @Test
    public void name() {
        helper.setMobileDataEnabled(true);
        assertTrue(helper.isConnectedtToInternet());
        helper.setMobileDataEnabled(false);
        assertFalse(helper.isConnectedtToInternet());
    }
}
