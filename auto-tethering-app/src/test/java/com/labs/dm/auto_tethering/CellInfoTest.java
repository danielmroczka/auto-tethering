package com.labs.dm.auto_tethering;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class CellInfoTest {

    @Test
    public void shouldParseValid() throws Exception {
        CellInfo cellInfo = new CellInfo(100, 200);
        String txt = cellInfo.toString();

        CellInfo cellInfo2 = new CellInfo(txt);
        assertEquals(cellInfo.getCid(), cellInfo2.getCid());
        assertEquals(cellInfo.getLac(), cellInfo2.getLac());
    }

    @Test
    public void shouldParseEmpty() throws Exception {
        CellInfo cellInfo = new CellInfo("");
        assertEquals(0, cellInfo.getCid());
        assertEquals(0, cellInfo.getLac());
    }

}