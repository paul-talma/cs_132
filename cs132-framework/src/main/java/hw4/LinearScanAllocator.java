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
// Given a sorted IntervalList and a fixed register file, assigns each
// variable either a register (if one is free) or a stack slot (spill).
public class LinearScanAllocator implements Allocator {
    List<Register> registers;
    List<String> registerNames;
    int numRegisters;

    // Per-call state, reset at the start of each allocate().
    Map<String, Home> homeOf;
    Queue<Register> freeRegisterPool;
    Active active;
    int stackOffset;

    // public LinearScanAllocator(List<String> registerNames) {
    // this.registerNames = registerNames;
    // this.numRegisters = registerNames.size();
    // }

    public LinearScanAllocator(List<Register> registers) {
        this.registers = registers;
        this.numRegisters = registers.size();
    }

    @Override
    public FunctionAllocation allocate(IntervalList intervals) {
        homeOf = new HashMap<>();
        freeRegisterPool = new ArrayDeque<Register>(registers);
        active = new Active();
        stackOffset = 0;

        for (Interval i : intervals.getIntervals()) {
            expireOldIntervals(i);
            if (active.length() == numRegisters) {
                spillAtInterval(i);
            } else {
                Register reg = freeRegisterPool.remove();
                homeOf.put(i.getVarName(), new Home(reg));
                active.add(i);
            }
        }

        return new FunctionAllocation(homeOf);
    }

    // Removes from active any interval that ends before i starts,
    // returning their registers to the free pool.
    private void expireOldIntervals(Interval i) {
        Iterator<Interval> it = active.getIntervals().iterator();
        while (it.hasNext()) {
            Interval j = it.next();
            if (j.getEnd() >= i.getStart())
                return;
            it.remove();
            freeRegisterPool.add(homeOf.get(j.getVarName()).getReg());
        }
    }

    // Called when all registers are taken. Spills either i or the active
    // interval with the furthest end point, whichever ends later.
    private void spillAtInterval(Interval i) {
        Interval spill = active.last();
        if (spill.getEnd() > i.getEnd()) {
            // spill the farthest-reaching active interval and give its register to i
            homeOf.put(i.getVarName(), homeOf.get(spill.getVarName()));
            homeOf.put(spill.getVarName(), new Home(new Identifier(spill.getVarName())));
            active.remove(spill);
            active.add(i);
        } else {
            // i ends even later, spill i directly
            homeOf.put(i.getVarName(), new Home(new Identifier(i.getVarName())));
        }
    }
}
