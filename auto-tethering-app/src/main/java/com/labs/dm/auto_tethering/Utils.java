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
        final String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
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

    public static String maskToDays(int mask) {
        String binary = Integer.toBinaryString(mask);
        String result = "";
        for (int i = 0; i < binary.length(); i++) {
            if ("1".equals(binary.substring(i, i + 1))) {
                switch (i) {
                    case 0:
                        result += "Mon ";
                        break;
                    case 1:
                        result += "Tue ";
                        break;
                    case 2:
                        result += "Wed ";
                        break;
                    case 3:
                        result += "Thu ";
                        break;
                    case 4:
                        result += "Fri ";
                        break;
                    case 5:
                        result += "Sat ";
                        break;
                    case 6:
                        result += "Sun ";
                        break;
                }
            }
        }
        result = result.trim().replaceAll(" ", ", ");
        return result;
    }
}
