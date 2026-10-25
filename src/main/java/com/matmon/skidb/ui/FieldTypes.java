package com.matmon.skidb.ui;

/** The field types supported in stage B, and conversion of typed text into their Java values. */
final class FieldTypes {

    static final String STRING = "String";
    static final String INTEGER = "Integer";
    static final String BOOLEAN = "Boolean";
    static final String[] ALL = {STRING, INTEGER, BOOLEAN};

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
            default -> true; // unknown type name: nothing to check against
        };
    }

    /** The type name of a value, for display: String, Integer, Boolean, or its class name. */
    static String nameOf(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
