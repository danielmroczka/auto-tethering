package com.labs.dm.auto_tethering.ui;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.db.CellGroup;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;

/**
 * Created by Daniel Mroczka on 2016-09-28.
 */

public class CellGroupPreference extends PreferenceGroup {

    private final PreferenceCategory parent;
    private CellGroup cellGroup;
    private DBManager db;

    public CellGroupPreference(PreferenceCategory parent, CellGroup cellGroup, Context context) {
        super(context, null);
        this.cellGroup = cellGroup;
        this.parent = parent;
        db = DBManager.getInstance(context);
        setTitle(cellGroup.getName());

    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        final ImageButton btnToogle = (ImageButton) view.findViewById(R.id.btnToggle);
        final ImageButton btnRemove = (ImageButton) view.findViewById(R.id.btnScheduleDelete);
        final LinearLayout middleLayout = (LinearLayout) view.findViewById(R.id.middleLayout);

        if (cellGroup.getStatus() == Cron.STATUS.SCHED_OFF_DISABLED.getValue()) {
            btnToogle.setSelected(false);
        } else if (cellGroup.getStatus() == Cron.STATUS.SCHED_OFF_ENABLED.getValue()) {
            btnToogle.setSelected(true);
        }

        btnToogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnToogle.setSelected(!btnToogle.isSelected());
                cellGroup.toggle();
                db.addOrUpdateCellGroup(cellGroup);
            }
        });

        btnRemove.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (db.removeCellGroup(cellGroup.getId()) > 0) {
                    parent.removePreference(CellGroupPreference.this);
                }
            }
        });


        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return true;
            }
        });

        middleLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.setOnClickListener(this);
//                Intent intent = new Intent(getContext(), CellGroupActivity.class);
//                intent.putExtra("groupId", cellGroup.getId());
//                if (getContext() instanceof Activity) {
//                    ((Activity) getContext()).startActivityForResult(intent, MainActivity.ON_CHANGE_CELLGROUP);
//                }
            }
        });
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.cell_group_item, parent, false);
    }


}
