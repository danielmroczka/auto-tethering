package com.labs.dm.auto_tethering;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is a helper for make development easier.
 * It should be rewrite in case of production usage.
 * <p>
 * Created by Daniel Mroczka on 2016-09-20.
 */

public class MyLog {
    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    private static final List<Item> log = new ArrayList<>();

    static class Item {
        private final int level;
        private final String content;

        Item(int level, String content) {
            this.level = level;
            this.content = formatter.format(new Date()) + " | " + content;
        }
    }

    enum LEVEL {
        debug(0), info(1), warn(2), error(3);

        private final int value;

        LEVEL(int level) {
            this.value = level;
        }
    }

    /**
     * 0 - debug
     * 1 - info
     * 2 - warn
     * 3 - error
     *
     * @param level
     * @return
     */
    public static String getContent(LEVEL level) {
        StringBuilder sb = new StringBuilder(log.size() / 2);

        for (Item item : log) {
            if (item.level >= level.value) {
                sb.append(item.content);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        add(new Item(1, formatMsg(msg)));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg != null ? msg : "");
        add(new Item(3, formatMsg(msg)));
    }

    public static void e(String tag, Exception ex) {
        e(tag, ex != null ? ex.getMessage() : "exception is null!");
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg, e);
        String context = formatMsg(msg) + "\n" + (e != null ? e.getMessage() : "exception is null!");
        add(new Item(3, context));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        add(new Item(2, formatMsg(msg)));
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        add(new Item(0, formatMsg(msg)));
    }

    private static void add(Item item) {
        if (BuildConfig.DEBUG) {
            if (log.size() > 200 && log.size() % 10 == 0) {
                for (int i = 0; i < 10; i++) {
                    log.remove(i);
                }
            }
            log.add(item);
        }
    }

    private static String formatMsg(String msg) {
        return msg == null ? "" : msg;
    }

    public static void clean() {
        log.clear();
    }

}
