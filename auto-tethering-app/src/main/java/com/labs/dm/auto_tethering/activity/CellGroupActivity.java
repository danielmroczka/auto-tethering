package com.labs.dm.auto_tethering.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;

public class CellGroupActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_cells);
        if (getIntent().getExtras() != null) {
            int groupId = getIntent().getIntExtra("groupId", 0);
            MyLog.i("Group", "ID:" + groupId);
        }
    }
}
