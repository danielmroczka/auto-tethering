package com.labs.dm.auto_tethering.service;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by Daniel Mroczka on 5/6/2016.
 */
public class ServiceActionTest {

    @Test
    public void general() throws Exception {
        assertTrue(ServiceAction.INTERNET_OFF.isInternet());
        assertTrue(!ServiceAction.INTERNET_OFF.isOn());
        assertTrue(!ServiceAction.INTERNET_OFF.isTethering());
    }
}