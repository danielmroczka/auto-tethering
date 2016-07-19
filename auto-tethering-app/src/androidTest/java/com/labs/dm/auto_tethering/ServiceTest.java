package com.labs.dm.auto_tethering;

import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Created by Daniel Mroczka on 2015-10-07.
 */
public class ServiceTest extends ServiceTestCase<TetheringService> {

    public ServiceTest() {
        super(TetheringService.class);
    }

    @SmallTest
    public void testOnCreate() throws Exception {
        Intent intent = new Intent(getContext(), TetheringService.class);
        startService(intent);
        assertNotNull(getService());

    }


}
