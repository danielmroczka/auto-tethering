package com.labs.dm.auto_tethering;

import android.app.Application;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Daniel Mroczka on 2016-09-20.
 */

public class MyLog extends Application {
    private static DateFormat formatter = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT);


    static class Item {
        int level;
        String content;

        Item(int level, String content) {
            this.level = level;
            this.content = formatter.format(new Date()) + " | " + content;
        }
    }

    private static List<Item> log = new ArrayList<>();

    /**
     * 0 - debug
     * 1 - info
     * 2 - warn
     * 3 - error
     *
     * @param level
     * @return
     */
    public static String getContent(int level) {
        StringBuilder sb = new StringBuilder(log.size() / 2);

        for (Item item : log) {
            if (item.level >= level) {
                sb.append(item.content);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        log.add(new Item(1, msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        log.add(new Item(3, msg));
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg, e);
        String context = msg + "\n" + e.getMessage();
        log.add(new Item(3, context));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        log.add(new Item(2, msg));
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        log.add(new Item(0, msg));
    }

}
