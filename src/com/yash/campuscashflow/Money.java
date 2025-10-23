package com.yash.campuscashflow;

public final class Money {

    // convert a text dollar string like "$12.34" to long cents (1234)
    public static long dollarsToCents(String text) {
        try {
            var clean = text.trim().replace("$", "");
            double d = Double.parseDouble(clean);
            return Math.round(d * 100.0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // parse a text dollar string to double dollars (e.g. "$12.34" → 12.34)
    public static double parseDollars(String text) {
        try {
            var clean = text.trim().replace("$", "");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // format long cents back to a dollar string
    public static String format(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }
}

