package com.labs.dm.auto_tethering;

import org.junit.Test;

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

}