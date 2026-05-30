package hw4;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Standard iterative liveness analysis.
//
// After analyze() returns, every ControlFlowNode in the graph has its
// liveIn and liveOut sets filled in.
//
// Equations (per node n):
//   out[n] = ∪ { in[s] | s ∈ succs(n) }
//   in[n]  = use[n] ∪ (out[n] − def[n])
//
// We seed the worklist with all nodes in reverse order (higher id first) so
// the first sweep is almost a complete backward pass, reducing iterations.
class LivenessAnalyzer {

    static void analyze(ControlFlowGraph cfg) {
        List<ControlFlowNode> nodes = cfg.nodes;

        // Initialise all sets to empty
        for (ControlFlowNode n : nodes) {
            n.liveIn  = new HashSet<>();
            n.liveOut = new HashSet<>();
        }

        // Worklist seeded in reverse order (approximates backward pass)
        Deque<ControlFlowNode> worklist = new ArrayDeque<>();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            worklist.addLast(nodes.get(i));
        }

        while (!worklist.isEmpty()) {
            ControlFlowNode n = worklist.removeFirst();

            // out[n] = ∪ in[s]
            Set<String> newOut = new HashSet<>();
            for (ControlFlowNode s : n.succs) {
                newOut.addAll(s.liveIn);
            }

            // in[n] = use[n] ∪ (out[n] − def[n])
            Set<String> newIn = new HashSet<>(newOut);
            newIn.removeAll(n.def);
            newIn.addAll(n.use);

            boolean changed = !newIn.equals(n.liveIn) || !newOut.equals(n.liveOut);
            if (changed) {
                n.liveIn  = newIn;
                n.liveOut = newOut;
                // Re-enqueue all predecessors whose out-set may have changed
                for (ControlFlowNode p : n.preds) {
                    if (!worklist.contains(p)) {
                        worklist.addLast(p);
                    }
                }
            }
        }
    }
}
