package hw4;

// Live interval for a single variable: the range [start, end] of instruction
// positions over which the variable must be kept in a register or spill slot.
class Interval {
    String var;
    int start;
    int end;
    boolean crossesCall = false;

    public Interval() {
    }

    // Interval with no end yet (will be extended by later uses).
    public Interval(String var, int start) {
        this.var = var;
        this.start = start;
    }

    public Interval(String var, int start, int end) {
        this.var = var;
        this.start = start;
        this.end = end;
    }

    String getVarName() {
        return var;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    void setEnd(int end) {
        this.end = end;
    }

    boolean getCrossesCall() {
        return crossesCall;
    }

    void setCrossesCall(boolean v) {
        this.crossesCall = v;
    }
}
