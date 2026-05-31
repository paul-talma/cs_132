package hw4;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import IR.syntaxtree.Instruction;

// One node in the control-flow graph.
// id matches the instruction's 0-based position in the function's instruction
// list; the return-value node uses id = instructions.size().
class ControlFlowNode {
    final int id;
    final Instruction instr; // null for the synthetic return node

    final Set<String> def = new LinkedHashSet<>();
    final Set<String> use = new LinkedHashSet<>();

    final Set<ControlFlowNode> succs = new LinkedHashSet<>();
    final Set<ControlFlowNode> preds = new LinkedHashSet<>();

    Set<String> liveIn = new HashSet<>();
    Set<String> liveOut = new HashSet<>();

    boolean isCall = false;
    int loopDepth = 0;

    ControlFlowNode(int id, Instruction instr) {
        this.id = id;
        this.instr = instr;
    }
}
