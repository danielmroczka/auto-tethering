package com.labs.dm.auto_tethering;

import android.content.Context;
import android.preference.CheckBoxPreference;

/**
 * Created by daniel on 2016-04-04.
 */
public class ScheduleCheckBoxPreference extends CheckBoxPreference {

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ScheduleCheckBoxPreference(Context context) {
        super(context);
    }
}
