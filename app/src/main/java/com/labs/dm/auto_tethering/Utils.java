package com.labs.dm.auto_tethering;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel Mroczka
 */
public class Utils {

    public static boolean validateTime(final String time) {
        String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
        Pattern pattern = Pattern.compile(TIME24HOURS_PATTERN);
        Matcher matcher = pattern.matcher(time);
        return matcher.matches();
    }

    public static int connectedClients() {
        int res = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("IP")) {
                    res++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    public static boolean exists(String commaSeparatedString, String item) {
        String[] sets = commaSeparatedString.split(",");
        for (String set : sets) {
            if (item != null && item.equals(set)) {
                return true;
            }
        }

        return false;
    }

    public static String add(String commaSeparatedString, String item) {
        if (!exists(commaSeparatedString, item)) {
            if (!commaSeparatedString.isEmpty()) {
                commaSeparatedString += ",";
            }
            commaSeparatedString += item;
        }

        return commaSeparatedString;
    }

    public static String remove(String commaSeparatedString, String item) {
        StringBuilder sb = new StringBuilder();
        for (String s : commaSeparatedString.split(",")) {
            if (!item.equalsIgnoreCase(s)) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }




}
