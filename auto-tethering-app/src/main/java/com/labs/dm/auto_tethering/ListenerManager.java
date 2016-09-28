package com.labs.dm.auto_tethering;

import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.activity.helpers.AbstractRegisterHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterAddSimCardListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterBatteryTemperatureListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterBluetoothListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterCellularListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterDataLimitListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterGeneralListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterSchedulerListenerHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Mroczka on 9/23/2016.
 */
public class ListenerManager {

    private Map<Class, AbstractRegisterHelper> map;
    private boolean created;
    private final MainActivity activity;

    public ListenerManager(MainActivity activity) {
        this.activity = activity;
    }

    private void create() {
        map = new HashMap<>();
        map.put(RegisterCellularListenerHelper.class, new RegisterCellularListenerHelper(activity));
        map.put(RegisterBatteryTemperatureListenerHelper.class, new RegisterBatteryTemperatureListenerHelper(activity));
        map.put(RegisterSchedulerListenerHelper.class, new RegisterSchedulerListenerHelper(activity));
        map.put(RegisterBluetoothListenerHelper.class, new RegisterBluetoothListenerHelper(activity));
        map.put(RegisterAddSimCardListenerHelper.class, new RegisterAddSimCardListenerHelper(activity));
        map.put(RegisterGeneralListenerHelper.class, new RegisterGeneralListenerHelper(activity));
        map.put(RegisterDataLimitListenerHelper.class, new RegisterDataLimitListenerHelper(activity));
        created = true;
    }

    public void registerAll() {
        if (!created) {
            create();
        }
        for (AbstractRegisterHelper helper : map.values()) {
            helper.registerUIListeners();
        }
    }

    public void unregisterAll() {
        for (AbstractRegisterHelper helper : map.values()) {
            helper.unregisterUIListeners();
        }
    }

    //TODO rewrite to use generic types
    public AbstractRegisterHelper getHelper(Class clazz) {
        return map.get(clazz);
    }

}
