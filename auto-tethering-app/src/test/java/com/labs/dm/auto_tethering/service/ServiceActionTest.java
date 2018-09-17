package com.labs.dm.auto_tethering.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Daniel Mroczka on 5/6/2016.
 */
public class ServiceActionTest {

    @Test
    public void general() {
        assertTrue(ServiceAction.INTERNET_OFF.isInternet());
        assertFalse(ServiceAction.INTERNET_OFF.isOn());
        assertFalse(ServiceAction.INTERNET_OFF.isTethering());
    }
}