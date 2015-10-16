package com.labs.dm.auto_tethering;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    public static int connectedClients() {
        int res = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                //line = line.trim();
                if (!line.startsWith("IP")) {
                    res++;
                    //line = line.substring(0, line.indexOf(" "));
                    //InetAddress address = InetAddress.getByName(line);
                    //if (address.isisReachable(100)) {
                    //items.add(line.trim());
                    //}
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }
}
