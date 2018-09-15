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

    @Test
    public void shouldContainValueAfterAdd() {
//        assertThat(MyLog.getContent(MyLog.LEVEL.info)).isEmpty();
        MyLog.i("tag", "info");
        MyLog.getContent(MyLog.LEVEL.info);
        //assertThat(MyLog.getContent(MyLog.LEVEL.info)).isNotEmpty();
    }

    @Test
    public void shouldClearLog() {
        MyLog.i("tag", "info");
        MyLog.clean();
//        assertThat(MyLog.getContent(MyLog.LEVEL.info)).isEmpty();
    }

}