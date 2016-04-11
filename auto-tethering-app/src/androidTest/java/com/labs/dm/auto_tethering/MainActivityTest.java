package com.labs.dm.auto_tethering;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.labs.dm.auto_tethering.activity.MainActivity;

/**
 * Created by Daniel Mroczka on 2015-10-11.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity activity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    @SmallTest
    public void testPreconditions() throws Exception {
        assertNotNull(activity);
    }

    @SmallTest
    public void testActivityHasFocus() {
        onView(hasFocus());
    }
}
