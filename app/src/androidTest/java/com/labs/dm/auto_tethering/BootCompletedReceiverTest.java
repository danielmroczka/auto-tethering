package com.labs.dm.auto_tethering;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        receiver = mock(BootCompletedReceiver.class);
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        receiver.onReceive(getContext(), intent);
        verify(receiver.onReceive(), atLeastOnce());
    }
}
