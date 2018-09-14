package com.labs.dm.auto_tethering;

import org.junit.Test;

public class MyLogTest {

    @Test
    public void shouldLogSupportNullValues() {
        MyLog.w(null, null);
        MyLog.i(null, null);
        MyLog.d(null, null);
        MyLog.e(null, (Exception) null);
        MyLog.e(null, (String) null);
        MyLog.e(null, null, null);
    }
}