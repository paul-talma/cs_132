package hw4;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import IR.syntaxtree.*;

// Forward must-constant analysis.
//
// knownIn[node] maps variable names to their statically known value
// (Integer for SetInteger, String for SetFuncName) immediately before
// the instruction at that node executes.  A variable is present in the
// map only if EVERY predecessor agrees on the same value; if any path
// disagrees (or a non-constant definition kills the variable) the entry
// is absent (treated as unknown).
//
// Only SetInteger and SetFuncName produce known values; all other
// definitions kill the variable from the map.
class ConstantAnalyzer {

    static void analyze(ControlFlowGraph cfg) {
        int n = cfg.size();

        // knownOut[i] = known values after node i; kept locally in the analyzer
        @SuppressWarnings("unchecked")
        Map<String, Object>[] knownOut = new HashMap[n];
        for (int i = 0; i < n; i++) {
            knownOut[i] = new HashMap<>();
            cfg.nodes.get(i).knownIn = new HashMap<>();
        }

        // Iterative worklist (all nodes initially queued)
        boolean[] inQueue = new boolean[n];
        Deque<ControlFlowNode> queue = new ArrayDeque<>();
        for (ControlFlowNode node : cfg.nodes) {
            queue.add(node);
            inQueue[node.id] = true;
        }

        while (!queue.isEmpty()) {
            ControlFlowNode node = queue.poll();
            inQueue[node.id] = false;

            // meet: intersect all predecessors' knownOut
            Map<String, Object> newIn = meet(node, knownOut);

            // transfer: apply this instruction's effect
            Map<String, Object> newOut = transfer(node, newIn);

            // propagate to successors if anything changed
            if (!newIn.equals(node.knownIn) || !newOut.equals(knownOut[node.id])) {
                node.knownIn = newIn;
                knownOut[node.id] = newOut;
                for (ControlFlowNode succ : node.succs) {
                    if (!inQueue[succ.id]) {
                        inQueue[succ.id] = true;
                        queue.add(succ);
                    }
                }
            }
        }
    }

    // Intersection of all predecessors' knownOut: keep only entries where
    // every predecessor agrees on the same value.
    private static Map<String, Object> meet(ControlFlowNode node,
                                             Map<String, Object>[] knownOut) {
        if (node.preds.isEmpty()) return new HashMap<>();
        Map<String, Object> result = null;
        for (ControlFlowNode pred : node.preds) {
            Map<String, Object> predOut = knownOut[pred.id];
            if (result == null) {
                result = new HashMap<>(predOut);
            } else {
                result.entrySet().removeIf(
                    e -> !e.getValue().equals(predOut.get(e.getKey())));
            }
        }
        return result != null ? result : new HashMap<>();
    }

    // Transfer function: produce knownOut from knownIn.
    //   SetInteger lhs = n   → lhs → Integer(n)
    //   SetFuncName lhs = @F → lhs → String("F")
    //   any other def        → kill that variable
    private static Map<String, Object> transfer(ControlFlowNode node,
                                                  Map<String, Object> in) {
        if (node.instr == null) return new HashMap<>(in); // synthetic return node

        Object choice = node.instr.f0.choice;
        Map<String, Object> out = new HashMap<>(in);

        if (choice instanceof SetInteger) {
            SetInteger si = (SetInteger) choice;
            out.put(si.f0.f0.tokenImage,
                    Integer.parseInt(si.f2.f0.tokenImage));

        } else if (choice instanceof SetFuncName) {
            SetFuncName sfn = (SetFuncName) choice;
            out.put(sfn.f0.f0.tokenImage, sfn.f3.f0.tokenImage);

        } else {
            // Kill every variable defined by this instruction.
            for (String v : node.def) out.remove(v);
        }

        return out;
    }
}
