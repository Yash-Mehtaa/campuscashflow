package com.yash.campuscashflow;

import java.util.ArrayList;
import java.util.List;

/** Super simple pub/sub for "data changed" events across tabs. */
public final class AppBus {
    private static final List<Runnable> listeners = new ArrayList<>();

    public static synchronized void subscribe(Runnable r) {
        listeners.add(r);
    }

    public static synchronized void unsubscribe(Runnable r) {
        listeners.remove(r);
    }

    /** Call this after any DB change (add/delete/etc). */
    public static synchronized void notifyDataChanged() {
        for (var r : List.copyOf(listeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
