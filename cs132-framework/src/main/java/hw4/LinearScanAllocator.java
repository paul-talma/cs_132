package hw4;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import IR.token.Identifier;
import IR.token.Register;

// Implements linear scan register allocation for a single function.
// Variables whose live interval crosses a call site are preferentially placed
// in callee-saved registers (s1–s11) so they survive calls without spilling.
// Variables that never cross a call are preferentially placed in caller-saved
// registers (t0–t3) to avoid unnecessary callee-save overhead.
public class LinearScanAllocator implements Allocator {
    List<Register> calleeRegisters; // s1–s11
    List<Register> callerRegisters; // t0–t3
    int numRegisters;

    // Per-call state, reset at the start of each allocate().
    Map<String, Home> homeOf;
    Queue<Register> freeCalleePool;
    Queue<Register> freeCallerPool;
    Active active;
    int stackOffset;

    public LinearScanAllocator(List<Register> calleeRegisters, List<Register> callerRegisters) {
        this.calleeRegisters = calleeRegisters;
        this.callerRegisters = callerRegisters;
        this.numRegisters = calleeRegisters.size() + callerRegisters.size();
    }

    public FunctionAllocation allocate(IntervalList intervals) {
        homeOf = new HashMap<>();
        freeCalleePool = new ArrayDeque<>(calleeRegisters);
        freeCallerPool = new ArrayDeque<>(callerRegisters);
        active = new Active();
        stackOffset = 0;

        for (Interval i : intervals.getIntervals()) {
            expireOldIntervals(i);
            if (freeCalleePool.isEmpty() && freeCallerPool.isEmpty()) {
                spillAtInterval(i);
            } else {
                Register reg = pickRegister(i);
                homeOf.put(i.getVarName(), new Home(reg));
                active.add(i);
            }
        }

        return new FunctionAllocation(homeOf);
    }

    // Pick a register for interval i, preferring the pool that matches its
    // cross-call status, falling back to the other pool if the preferred is empty.
    private Register pickRegister(Interval i) {
        if (i.getCrossesCall()) {
            if (!freeCalleePool.isEmpty()) return freeCalleePool.remove();
            return freeCallerPool.remove();
        } else {
            if (!freeCallerPool.isEmpty()) return freeCallerPool.remove();
            return freeCalleePool.remove();
        }
    }

    // Removes from active any interval that ends before i starts,
    // returning their registers to the appropriate free pool.
    private void expireOldIntervals(Interval i) {
        Iterator<Interval> it = active.getIntervals().iterator();
        while (it.hasNext()) {
            Interval j = it.next();
            if (j.getEnd() >= i.getStart())
                return;
            it.remove();
            Register reg = homeOf.get(j.getVarName()).getReg();
            if (isCalleeReg(reg)) {
                freeCalleePool.add(reg);
            } else {
                freeCallerPool.add(reg);
            }
        }
    }

    // Called when all registers are taken. Spills either i or the active
    // interval with the furthest end point, whichever ends later.
    private void spillAtInterval(Interval i) {
        Interval spill = active.last();
        if (spill.getEnd() > i.getEnd()) {
            homeOf.put(i.getVarName(), homeOf.get(spill.getVarName()));
            homeOf.put(spill.getVarName(), new Home(new Identifier(spill.getVarName())));
            active.remove(spill);
            active.add(i);
        } else {
            homeOf.put(i.getVarName(), new Home(new Identifier(i.getVarName())));
        }
    }

    private boolean isCalleeReg(Register r) {
        String name = r.toString();
        for (Register cs : calleeRegisters) {
            if (cs.toString().equals(name)) return true;
        }
        return false;
    }
}
