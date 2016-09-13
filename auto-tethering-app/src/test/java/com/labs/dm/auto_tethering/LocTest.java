package com.labs.dm.auto_tethering;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class LocTest {

    @Test
    public void shouldParseValid() throws Exception {
        Loc loc = new Loc(100, 200);
        String txt = loc.toString();

        Loc loc2 = new Loc(txt);
        assertEquals(loc.getCid(), loc2.getCid());
        assertEquals(loc.getLac(), loc2.getLac());
    }

    @Test
    public void shouldParseEmpty() throws Exception {
        Loc loc = new Loc("");
        assertEquals(0, loc.getCid());
        assertEquals(0, loc.getLac());
    }

}