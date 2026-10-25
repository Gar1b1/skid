package com.matmon.skidb.api;

/**
 * One query condition on a field, for example {@code age >= 18} or {@code age between 18 and 25}.
 * A query returns the records that satisfy ALL its conditions.
 * <p>
 * Comparisons use the field's natural order: numbers for Integer, alphabetical
 * ({@link String#compareTo}) for String. Range operators are not used on Boolean fields.
 *
 * @param field    the field name
 * @param operator how to compare the field's value
 * @param value    the value to compare with; for {@link Operator#BETWEEN} the lower bound
 * @param to       for {@link Operator#BETWEEN} only: the upper bound; null for every other operator
 */
public record Condition(String field, Operator operator, Object value, Object to) {

    /** How a condition compares a record's field value with the condition's value. */
    public enum Operator {
        /** field = value */
        EQUALS("="),
        /** field &gt; value */
        GREATER_THAN(">"),
        /** field &gt;= value */
        GREATER_OR_EQUAL(">="),
        /** field &lt; value */
        LESS_THAN("<"),
        /** field &lt;= value */
        LESS_OR_EQUAL("<="),
        /** value &lt;= field &lt;= to (both bounds included) */
        BETWEEN("between");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        /** @return how the operator is written, for example "&gt;=" */
        public String symbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    /** @return the condition as text, for example {@code age between 18 and 25} */
    @Override
    public String toString() {
        return field + " " + operator.symbol() + " " + text(value) + (operator == Operator.BETWEEN ? " and " + text(to) : "");
    }

    private static String text(Object v) {
        return v instanceof String s ? "\"" + s + "\"" : String.valueOf(v);
    }
}
