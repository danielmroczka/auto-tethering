package com.labs.dm.auto_tethering;

import android.net.wifi.WifiConfiguration;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.labs.dm.auto_tethering.service.ServiceHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WifiManagerTest {

    private final ServiceHelper helper = new ServiceHelper(InstrumentationRegistry.getTargetContext());
    private final int TIMEOUT = 2000;

    @Test
    public void startTethering() {
        helper.setWifiTethering(true, getWifiConfiguration());
        SystemClock.sleep(TIMEOUT);
        assertTrue(helper.isTetheringWiFi());

        helper.setWifiTethering(false, null);
        SystemClock.sleep(TIMEOUT);
        assertFalse(helper.isTetheringWiFi());
    }

    @Test
    public void startTwoTimesTethering() {
        helper.setWifiTethering(true, getWifiConfiguration());
        SystemClock.sleep(TIMEOUT);
        assertTrue(helper.isTetheringWiFi());
        helper.setWifiTethering(true, getWifiConfiguration());
        SystemClock.sleep(TIMEOUT);
        assertTrue(helper.isTetheringWiFi());
        helper.setWifiTethering(false, null);
        SystemClock.sleep(TIMEOUT);
        assertFalse(helper.isTetheringWiFi());
        helper.setWifiTethering(false, null);
        SystemClock.sleep(TIMEOUT);
        assertFalse(helper.isTetheringWiFi());

    }

    public static WifiConfiguration getWifiConfiguration() {
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = "test1234";
        netConfig.preSharedKey = "test1234";
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN | WifiConfiguration.AuthAlgorithm.SHARED);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN | WifiConfiguration.Protocol.WPA);
        return netConfig;
    }
}
