package hw4;

import java.util.HashMap;
import java.util.Map;

import IR.token.Identifier;

class TrivialAllocator implements Allocator {
    Map<String, Home> homeOf;

    public TrivialAllocator() {
    }

    public FunctionAllocation allocate(IntervalList intervals) {
        homeOf = new HashMap<String, Home>();

        for (Interval i : intervals.getIntervals()) {
            String varName = i.getVarName();
            homeOf.put(varName, new Home(new Identifier(varName)));
        }
        return new FunctionAllocation(homeOf);
    }
}
