package com.labs.dm.auto_tethering;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testCommaSeparatedUtils() throws Exception {
        String text = new String("");

        text = Utils.add(text, "123");
        text = Utils.add(text, "1234");
        text = Utils.add(text, "12345");

        assertTrue(Utils.exists(text, "123"));
        assertTrue(Utils.exists(text, "1234"));
        assertTrue(Utils.exists(text, "12345"));

        assertFalse(Utils.exists(text, "345"));

        text = Utils.remove(text, "1234");
        assertFalse(Utils.exists(text, "1234"));
        text = Utils.remove(text, "123");
        assertFalse(Utils.exists(text, "123"));
        text = Utils.remove(text, "12345");
        assertFalse(Utils.exists(text, "12345"));

        assertEquals("", text);
    }
}