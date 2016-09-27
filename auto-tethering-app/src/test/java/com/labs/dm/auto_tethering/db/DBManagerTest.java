package com.labs.dm.auto_tethering.db;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.activity.MainActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Daniel Mroczka on 2016-09-27.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@Ignore
public class DBManagerTest {
    private MainActivity activity;
    private DBManager db;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(MainActivity.class)
                .create()
                .resume()
                .get();
        db = DBManager.getInstance(activity);
    }

    @Test
    public void shouldAddCron() throws Exception {
        Cron cron = new Cron(9, 0, 12, 0, 0, 0);
        long res = db.addOrUpdateCron(cron);
        assertTrue(res > 0);
    }

    @Test
    public void shouldGetCrons() throws Exception {
        Cron cron = new Cron(9, 0, 12, 0, 0, 0);
        long id = db.addOrUpdateCron(cron);

        assertEquals(1, db.getCrons().size());
        assertEquals(id, db.getCrons().get(0).getId());
    }

}