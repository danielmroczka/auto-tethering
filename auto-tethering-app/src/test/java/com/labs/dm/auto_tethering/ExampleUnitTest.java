package com.labs.dm.auto_tethering;

import android.content.Context;
import android.content.Intent;
import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering.service.TetheringService;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    @Test
    public void dummyTest() throws Exception {
        assertTrue(true);
    }

    @Test
    @Ignore
    public void testStartActivity() {
        BootCompletedReceiver receiver = mock(BootCompletedReceiver.class);
        Context context = mock(Context.class);
        TetheringService service = mock(TetheringService.class);
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        receiver.onReceive(context, intent);
        verify(service, atLeastOnce());
    }
}