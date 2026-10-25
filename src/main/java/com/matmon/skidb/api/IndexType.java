package com.matmon.skidb.api;

/** The kind of data structure behind an index. */
public enum IndexType {

    /** A hash map (for example HashMap): finds one value in O(1). Good for "=" conditions. */
    HASH,

    /**
     * A sorted tree map (for example TreeMap): finds one value in O(log n) and keeps values in
     * order, so it can also answer range conditions (&gt;, &gt;=, &lt;, &lt;=, between).
     * {@link SkiDBAdapter#getIndex} must return a {@link java.util.SortedMap} for a TREE index.
     */
    TREE
}
