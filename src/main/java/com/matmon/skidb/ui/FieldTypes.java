package com.matmon.skidb.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** The field types, conversion of typed text into their Java values, and hour partition ids. */
final class FieldTypes {

    static final String STRING = "String";
    static final String INTEGER = "Integer";
    static final String BOOLEAN = "Boolean";
    static final String DATETIME = "DateTime";
    static final String[] ALL = {STRING, INTEGER, BOOLEAN, DATETIME};

    private static final DateTimeFormatter HOUR_ID = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    private FieldTypes() {
    }

    /**
     * Converts form text to the Java value sent to the adapter: String, Integer or Boolean.
     *
     * @throws IllegalArgumentException if the text is not a valid value of that type
     */
    static Object parse(String type, String text) {
        switch (type) {
            case INTEGER -> {
                try {
                    return Integer.parseInt(text.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\"" + text + "\" is not a valid Integer");
                }
            }
            case BOOLEAN -> {
                String t = text.trim();
                if (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(t);
                }
                throw new IllegalArgumentException("\"" + text + "\" is not a valid Boolean (use true or false)");
            }
            case DATETIME -> {
                try {
                    return LocalDateTime.parse(text.trim().replace(' ', 'T'));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("\"" + text + "\" is not a valid DateTime (use 2026-10-25 14:30)");
                }
            }
            default -> {
                return text;
            }
        }
    }

    /** @return true if {@code value} is an instance of the Java class for {@code type} */
    static boolean matches(String type, Object value) {
        return switch (type == null ? "" : type) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof Integer;
            case BOOLEAN -> value instanceof Boolean;
            case DATETIME -> value instanceof LocalDateTime;
            default -> true; // unknown type name: nothing to check against
        };
    }

    /** The type name of a value, for display: String, Integer, Boolean, DateTime, or its class name. */
    static String nameOf(Object value) {
        return value == null ? "null" : value instanceof LocalDateTime ? DATETIME : value.getClass().getSimpleName();
    }

    /** The id of the hour partition a time belongs to, for example "2026-10-25T14". */
    static String hourId(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.HOURS).format(HOUR_ID);
    }

    /** The first moment of an hour partition, or null if {@code id} is not a valid partition id. */
    static LocalDateTime hourStart(String id) {
        try {
            return LocalDateTime.parse(id + ":00");
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
