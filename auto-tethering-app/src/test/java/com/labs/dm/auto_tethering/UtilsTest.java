package com.labs.dm.auto_tethering;

import org.junit.Test;

import static org.junit.Assert.*;

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
}