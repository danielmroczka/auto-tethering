package com.labs.dm.auto_tethering;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;

/**
 * Created by daniel on 2015-10-08.
 */
public class BootCompletedReceiverTest extends AndroidTestCase {
    private BootCompletedReceiver receiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        receiver = new BootCompletedReceiver();
    }

    @SmallTest
    public void testStartActivity() {
        receiver = new BootCompletedReceiver();
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        //intent.putExtra(Intent.EXTRA_PHONE_NUMBER, "01234567890");

        receiver.onReceive(getContext(), intent);
    }
}
