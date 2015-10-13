package com.labs.dm.auto_tethering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by daniel on 2015-10-13.
 */
public class Utils {

    public static boolean validateTime(final String time) {
        String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
        Pattern pattern = Pattern.compile(TIME24HOURS_PATTERN);
        Matcher matcher = pattern.matcher(time);
        return matcher.matches();
    }
}
