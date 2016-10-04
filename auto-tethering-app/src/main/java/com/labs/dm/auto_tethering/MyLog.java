package com.labs.dm.auto_tethering;

import android.app.Application;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is a helper for make development easier.
 * It should be rewrite in case of production usage.
 *
 * Created by Daniel Mroczka on 2016-09-20.
 */

public class MyLog extends Application {
    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    static class Item {
        final int level;
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
        add(new Item(1, tag + " " + msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        add(new Item(3, tag + " " + msg));
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg, e);
        String context = tag + " " + msg + "\n" + e.getMessage();
        add(new Item(3, context));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        add(new Item(2, tag + " " + msg));
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        add(new Item(0, tag + " " + msg));
    }

    private static void add(Item item) {
        if (BuildConfig.DEBUG) {
            log.add(item);
        }
    }

    public static void clean() {
        log.clear();
    }

}
