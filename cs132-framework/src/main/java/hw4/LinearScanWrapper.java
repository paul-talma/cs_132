package hw4;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.syntaxtree.FunctionDeclaration;
import IR.token.Register;

// Wraps LinearScanVisitor + LinearScanAllocator to implement FunctionAllocator,
// providing the same interface as ChordalAllocator for use by the Translator.
public class LinearScanWrapper implements FunctionAllocator {

    private static final List<Register> CALLEE_REGS = Arrays.asList(
            new Register("s1"),  new Register("s2"),  new Register("s3"),
            new Register("s4"),  new Register("s5"),  new Register("s6"),
            new Register("s7"),  new Register("s8"),  new Register("s9"),
            new Register("s10"), new Register("s11"));
    private static final List<Register> CALLER_REGS = Arrays.asList(
            new Register("t0"), new Register("t1"),
            new Register("t2"), new Register("t3"));

    private final LinearScanAllocator lsa =
            new LinearScanAllocator(CALLEE_REGS, CALLER_REGS);

    // Per-function state
    private IntervalList intervals;
    private List<Integer> callPositions;

    @Override
    public FunctionAllocation allocate(FunctionDeclaration n) {
        LinearScanVisitor lsv = new LinearScanVisitor();
        lsv.visit(n);
        intervals     = lsv.getIntervalList();
        callPositions = lsv.callPositions;
        return lsa.allocate(intervals);
    }

    // A variable is live at function entry if its interval starts at position 0.
    @Override
    public boolean isLiveAtEntry(String varName) {
        if (intervals == null) return false;
        for (Interval iv : intervals.getIntervals())
            if (iv.getVarName().equals(varName)) return iv.getStart() == 0;
        return false;
    }

    // A variable is live across the call at instrIdx if its interval spans it.
    @Override
    public Set<String> getLiveOutAt(int instrIdx) {
        if (intervals == null) return Collections.emptySet();
        Set<String> live = new HashSet<>();
        for (Interval iv : intervals.getIntervals())
            if (iv.getStart() <= instrIdx && iv.getEnd() > instrIdx)
                live.add(iv.getVarName());
        return live;
    }

    // Devirtualization is only performed by the chordal allocator.
    @Override
    public Map<String, String> getDevirtMap() {
        return Collections.emptyMap();
    }
}
