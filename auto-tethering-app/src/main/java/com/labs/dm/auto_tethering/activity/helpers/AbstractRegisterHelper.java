package com.labs.dm.auto_tethering.activity.helpers;

import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spanned;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.DBManager;

/**
 * Created by Daniel Mroczka on 9/21/2016.
 */
public abstract class AbstractRegisterHelper {
    protected final MainActivity activity;
    protected final SharedPreferences prefs;
    protected final DBManager db;

    protected AbstractRegisterHelper(MainActivity activity) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.db = DBManager.getInstance(activity);
    }

    public abstract void registerUIListeners();

    public void unregisterUIListeners() {
    }

    public void prepare() {

    }

    protected Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((String) newValue);
            return true;
        }
    };

    protected class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (min <= input && input <= max || (dest.length() + source.length() < 2)) {
                    return null;
                }
            } catch (NumberFormatException nfe) {
                MyLog.e("InputFilterMinMax", nfe.getMessage());
            }
            return "";
        }
    }

    public int getItemsCount(PreferenceCategory pc, String prefix) {
        int result = 0;
        for (int i = 0; i < pc.getPreferenceCount(); i++) {
            if (pc.getPreference(i) instanceof CheckBoxPreference) {
                result++;
            }
        }
        return result;
    }

    protected PreferenceScreen getPreferenceScreen(String name) {
        return (PreferenceScreen) activity.findPreference(name);
    }

    protected EditTextPreference getEditTextPreference(String name) {
        return (EditTextPreference) activity.findPreference(name);
    }

    protected CheckBoxPreference getCheckBoxPreference(String name) {
        return (CheckBoxPreference) activity.findPreference(name);
    }

    protected PreferenceCategory getPreferenceCategory(String name) {
        return (PreferenceCategory) activity.findPreference(name);
    }
}
