package hw4;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IR.syntaxtree.*;
import IR.visitor.DepthFirstVisitor;

// Walks a single Sparrow function and computes a live interval for every
// variable in it. Instantiate once per function; call visit(FunctionDeclaration)
// or visit the Block directly, then retrieve the result via getIntervalList().
class LinearScanVisitor extends DepthFirstVisitor {
    // Per-function state reset at the start of each FunctionDeclaration.

    int pos = 0; // current instruction position
    Map<String, Integer> labelMap; // label name → instruction position
    IntervalList intervalList = new IntervalList();
    List<Integer> callPositions; // instruction positions of Call instructions
    String functionName;

    // getters & setters

    IntervalList getIntervalList() {
        return intervalList;
    }

    // visitors
    public void visit(FunctionDeclaration n) {
        pos = 0;
        labelMap = new HashMap<String, Integer>();
        intervalList = new IntervalList();
        callPositions = new ArrayList<>();
        functionName = n.f1.f0.tokenImage;

        // pre-create intervals for formal parameters so their start is always 0
        if (n.f3.present()) {
            for (Enumeration<IR.syntaxtree.Node> e = n.f3.elements(); e.hasMoreElements();) {
                intervalList.add(new Interval(name((Identifier) e.nextElement()), 0));
            }
        }

        n.f5.accept(this);

        // second pass: mark any interval that straddles a call site
        for (int callPos : callPositions) {
            for (Interval interval : intervalList.getIntervals()) {
                if (interval.getStart() <= callPos && interval.getEnd() > callPos) {
                    interval.setCrossesCall(true);
                }
            }
        }
    }

    public void visit(IR.syntaxtree.Block n) {
        n.f0.accept(this);
        // treat the return identifier as a use so its interval extends to
        // end-of-function
        processUse(name(n.f2));
    }

    public void visit(LabelWithColon n) {
        labelMap.put(n.f0.f0.tokenImage, pos);
        pos += 1;
    }

    public void visit(SetInteger n) {
        processDef(name(n.f0));
        pos += 1;
    }

    public void visit(SetFuncName n) {
        processDef(name(n.f0));
        pos += 1;
    }

    public void visit(Add n) {
        processDef(name(n.f0));
        processUse(name(n.f2));
        processUse(name(n.f4));
        pos += 1;
    }

    public void visit(Subtract n) {
        processDef(name(n.f0));
        processUse(name(n.f2));
        processUse(name(n.f4));
        pos += 1;
    }

    public void visit(Multiply n) {
        processDef(name(n.f0));
        processUse(name(n.f2));
        processUse(name(n.f4));
        pos += 1;
    }

    public void visit(LessThan n) {
        processDef(name(n.f0));
        processUse(name(n.f2));
        processUse(name(n.f4));
        pos += 1;
    }

    public void visit(Load n) {
        processDef(name(n.f0));
        processUse(name(n.f3));
        pos += 1;
    }

    public void visit(Store n) {
        processUse(name(n.f1));
        processUse(name(n.f6));
        pos += 1;
    }

    public void visit(Move n) {
        processDef(name(n.f0));
        processUse(name(n.f2));
        pos += 1;
    }

    public void visit(Alloc n) {
        processDef(name(n.f0));
        processUse(name(n.f4));
        pos += 1;
    }

    public void visit(Print n) {
        processUse(name(n.f2));
        pos += 1;
    }

    public void visit(ErrorMessage n) {
        pos += 1;
    }

    public void visit(Goto n) {
        String label = n.f1.f0.tokenImage;
        processLabel(labelMap.get(label));
        pos += 1;
    }

    public void visit(IfGoto n) {
        processUse(name(n.f1));
        String label = n.f3.f0.tokenImage;
        processLabel(labelMap.get(label));
        pos += 1;
    }

    public void visit(Call n) {
        processDef(name(n.f0));
        processUse(name(n.f3));
        if (n.f5.present()) {
            for (Enumeration<IR.syntaxtree.Node> e = n.f5.elements(); e.hasMoreElements();) {
                processUse(name((Identifier) e.nextElement()));
            }
        }
        callPositions.add(pos);
        pos += 1;
    }

    // helpers

    private String name(Identifier id) {
        return id.f0.tokenImage;
    }

    // Record a definition of var at the current position.
    // No-op if an interval for var already exists (e.g. previously defined).
    void processDef(String var) {
        if (!intervalList.contains(var)) {
            intervalList.add(new Interval(var, pos));
        }
    }

    // Extends (or creates) the interval for var to cover the current position.
    // A use before any def gets start=0 to indicate liveness from function entry.
    void processUse(String var) {
        if (!intervalList.contains(var)) { // probably redundant since we preallocate params
            intervalList.add(new Interval(var, 0, pos));
        } else {
            intervalList.extendEnd(var, pos);
        }
    }

    // When a backward branch to labelPos exists, any variable whose interval
    // straddles the branch target must stay live until the branch site (pos),
    // because a second trip through the loop body may read it again.
    void processLabel(Integer labelPos) {
        if (labelPos == null) // forward jump
            return;
        List<String> toExtend = new ArrayList<>();
        for (Interval interval : intervalList.getIntervals()) {
            int start = interval.getStart();
            int end = interval.getEnd();
            if (start < labelPos && labelPos < end) {
                toExtend.add(interval.getVarName());
            }
        }
        for (String var : toExtend) {
            intervalList.extendEnd(var, pos);
        }
    }

    void prettyPrint() {
        System.err.println("Function: " + functionName);
        System.err.println("  Variable        Start  End");
        System.err.println("  --------------- -----  ---");

        for (Interval interval : intervalList.getIntervals()) {
            System.err.printf("  %-15s %5d  %d%n",
                    interval.getVarName(),
                    interval.getStart(),
                    interval.getEnd());
        }

        System.err.println();
    }
}
