package hw4;

import java.util.Comparator;
import java.util.TreeSet;

class Active {
    Comparator<Interval> cmp = Comparator.comparingInt(Interval::getEnd)
            .thenComparingInt(Interval::getStart)
            .thenComparing(Interval::getVarName);

    TreeSet<Interval> active = new TreeSet<Interval>(cmp);

    public Active() {

    }

    void add(Interval i) {
        active.add(i);
    }

    void remove(Interval i) {
        active.remove(i);
    }

    Interval last() {
        return active.last();
    }

    int length() {
        return active.size();
    }

    TreeSet<Interval> getIntervals() {
        return active;
    }
}
