package com.labs.dm.auto_tethering;

import android.content.SharedPreferences;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Created by Daniel Mroczka
 */
public class UtilsTest {

    @Test
    public void testValidateTime() throws Exception {
        assertTrue(Utils.validateTime("0:00"));
        assertTrue(Utils.validateTime("23:59"));
        assertFalse(Utils.validateTime("24:60"));
        assertFalse(Utils.validateTime("123:123"));
    }

    @Test
    public void testMaskToDays() throws Exception {
        assertEquals("", Utils.maskToDays(0));
        assertEquals("Mon", Utils.maskToDays(1));
        assertEquals("Tue", Utils.maskToDays(2));
        assertEquals("Mon, Tue", Utils.maskToDays(3));
        assertEquals("Mon, Tue, Wed", Utils.maskToDays(7));
        assertEquals("Sun", Utils.maskToDays(64));
    }

    @Test
    public void testFindPreferredDevices() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        Map<String, ?> map = new HashMap<>();
        List<String> devices = Utils.findPreferredDevices(prefs);
    }
}