package com.matmon.skidb.ui;

import com.matmon.skidb.api.Condition;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The UI's own evaluation of query conditions, used to check the student's query results by a full scan. */
final class ConditionCheck {

    private ConditionCheck() {
    }

    static boolean matchesAll(Map<String, Object> record, List<Condition> conditions) {
        return record != null && conditions.stream().allMatch(c -> matches(c, record.get(c.field())));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean matches(Condition c, Object v) {
        if (v == null) {
            return false;
        }
        if (c.operator() == Condition.Operator.EQUALS) {
            return Objects.equals(v, c.value());
        }
        if (!(v instanceof Comparable) || !sameClass(v, c.value())
                || (c.operator() == Condition.Operator.BETWEEN && !sameClass(v, c.to()))) {
            return false;
        }
        int cmp = ((Comparable) v).compareTo(c.value());
        return switch (c.operator()) {
            case GREATER_THAN -> cmp > 0;
            case GREATER_OR_EQUAL -> cmp >= 0;
            case LESS_THAN -> cmp < 0;
            case LESS_OR_EQUAL -> cmp <= 0;
            case BETWEEN -> cmp >= 0 && ((Comparable) v).compareTo(c.to()) <= 0;
            case EQUALS -> true; // handled above
        };
    }

    private static boolean sameClass(Object a, Object b) {
        return b != null && a.getClass() == b.getClass();
    }
}
