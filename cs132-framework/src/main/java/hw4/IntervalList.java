package hw4;

import java.util.Comparator;
import java.util.TreeSet;

// The set of live intervals for a single function, kept sorted by start point
// (ties broken by end, then variable name) for the linear scan outer loop.
// The Active set is separately sorted by end point.
class IntervalList {
    static final Comparator<Interval> ORDER = Comparator
            .comparingInt(Interval::getStart)
            .thenComparingInt(Interval::getEnd)
            .thenComparing(Interval::getVarName);

    TreeSet<Interval> intervals = new TreeSet<>(ORDER);

    void add(Interval i) {
        intervals.add(i);
    }

    TreeSet<Interval> getIntervals() {
        return intervals;
    }

    // Returns true if an interval for var already exists.
    boolean contains(String var) {
        for (Interval i : intervals) {
            if (i.getVarName().equals(var)) {
                return true;
            }
        }
        return false;
    }

    Interval getIntervalForVar(String var) {
        for (Interval i : intervals) {
            if (i.getVarName().equals(var)) {
                return i;
            }
        }
        return null;
    }

    // Extends the end of var's interval to pos. The interval must be removed
    // and re-inserted because end is part of the TreeSet's sort key.
    void extendEnd(String var, int pos) {
        Interval interval = getIntervalForVar(var);
        intervals.remove(interval);
        interval.setEnd(pos);
        intervals.add(interval);
    }
}
