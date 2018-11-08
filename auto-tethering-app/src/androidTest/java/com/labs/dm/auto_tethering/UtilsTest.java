package com.labs.dm.auto_tethering;

import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.labs.dm.auto_tethering.db.Cellular;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UtilsTest {

    @Test
    public void connectedClients() {
        assertEquals(0, Utils.connectedClients());
    }

    @Test
    public void getBestLocation() {
        Location location = Utils.getBestLocation(InstrumentationRegistry.getTargetContext());
        assertNotNull(location);
    }

    @Test
    public void getCellInfo() {
        Cellular cellular = Utils.getCellInfo(InstrumentationRegistry.getTargetContext());
        assertTrue(cellular.isValid());
    }

}