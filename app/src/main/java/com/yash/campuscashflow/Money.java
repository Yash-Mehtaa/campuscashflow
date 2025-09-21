package com.yash.campuscashflow;

public final class Money {
    public static long dollarsToCents(String text) {
        var clean = text.trim().replace("$","");
        double d = Double.parseDouble(clean);
        return Math.round(d * 100.0);
    }
    public static String format(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }
}
