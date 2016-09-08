package com.labs.dm.auto_tethering;

import android.content.SharedPreferences;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  /*  @Test
    public void testFindPreferredDevices() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        Map<String, ?> map = new HashMap<>();
       // List<String> devices = Utils.findPreferredDevices(prefs);
    }*/

    @Test
    public void testFindPreferredDevices() throws Exception {
        SharedPreferences preferences = mock(SharedPreferences.class);
        Map map = new HashMap<>();
        map.put("bt.devices.ITEM1", "ITEM1");
        map.put("bt.devices.ITEM2", "ITEM2");
        map.put("bt.devices.ITEM3", "ITEM3");

        when(preferences.getAll()).thenReturn(map);
        when(preferences.getLong("bt.last.connect.ITEM1", 0)).thenReturn(100L);
        when(preferences.getLong("bt.last.connect.ITEM2", 0)).thenReturn(0L);
        when(preferences.getLong("bt.last.connect.ITEM3", 0)).thenReturn(10000L);
        List<String> list = Utils.findPreferredDevices(preferences);
        assertEquals("ITEM3", list.get(0));
        assertEquals("ITEM1", list.get(1));
        assertEquals("ITEM2", list.get(2));
    }
}