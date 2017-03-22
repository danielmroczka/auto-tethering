package com.labs.dm.auto_tethering.db;

import com.labs.dm.auto_tethering.db.WiFiTethering.SECURITY_TYPE;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by daniel on 2017-03-21.
 */
public class WiFiTetheringTest {
    @Test
    public void name() throws Exception {
        assertEquals(SECURITY_TYPE.WPAPSK, SECURITY_TYPE.valueOf("WPAPSK"));
        assertEquals(SECURITY_TYPE.WPA2PSK, SECURITY_TYPE.valueOf("WPA2PSK"));
        assertEquals(SECURITY_TYPE.OPEN, SECURITY_TYPE.valueOf("OPEN"));
    }

    @Test
    public void code() throws Exception {
        assertEquals(4, SECURITY_TYPE.WPA2PSK.getCode());
    }
}