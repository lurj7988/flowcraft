package com.original.flowcraft.utils;

public class StringUtils {
    public static boolean isBlank(final Object string) {
        boolean blank = false;
        if (string == null) {
            blank = true;
        } else if (string instanceof String) {
            blank = string.toString().trim().isEmpty() || "null".equalsIgnoreCase(string.toString());
        }
        return blank;
    }

    public static boolean isNotBlank(final Object string) {
        return !isBlank(string);
    }
}
