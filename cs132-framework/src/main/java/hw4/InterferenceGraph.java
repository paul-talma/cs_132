package hw4;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Undirected interference graph over variable names.
//
// Two variables interfere when they cannot share the same register.
// Standard rule: variable d defined at node n interferes with every variable
// v in liveOut[n] (except d itself).
class InterferenceGraph {

    // Adjacency sets (symmetric)
    private final Map<String, Set<String>> adj = new LinkedHashMap<>();

    // ----- construction -----

    void addNode(String v) {
        adj.computeIfAbsent(v, k -> new LinkedHashSet<>());
    }

    void addEdge(String u, String v) {
        if (u.equals(v)) return;
        addNode(u);
        addNode(v);
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    // Build the interference graph from a CFG that has had liveness computed.
    static InterferenceGraph build(ControlFlowGraph cfg) {
        InterferenceGraph ig = new InterferenceGraph();

        // Ensure every variable has a node
        for (ControlFlowNode n : cfg.nodes) {
            for (String v : n.def)    ig.addNode(v);
            for (String v : n.use)    ig.addNode(v);
            for (String v : n.liveIn) ig.addNode(v);
        }

        // For each def d at node n, d interferes with every live-out var at n.
        for (ControlFlowNode n : cfg.nodes) {
            for (String d : n.def) {
                for (String v : n.liveOut) {
                    ig.addEdge(d, v);
                }
            }
        }

        // Parameters are simultaneously live at function entry (they arrive
        // from the caller before any instruction executes).  None of them
        // appears in a CFG def set, so the rule above would never add edges
        // between them — but they must all live in distinct registers.
        // Model this by treating the function entry as a virtual node that
        // defines all parameters with liveOut = liveIn[first instruction].
        if (!cfg.params.isEmpty() && !cfg.nodes.isEmpty()) {
            Set<String> entryLive = cfg.nodes.get(0).liveIn;
            // Each parameter interferes with every variable live at entry.
            for (String p : cfg.params) {
                for (String v : entryLive) {
                    ig.addEdge(p, v);  // also handles pairwise param edges
                }
                ig.addNode(p); // ensure dead params still appear
            }
        }

        return ig;
    }


    void removeNode(String v) {
        Set<String> neighbors = adj.remove(v);
        if (neighbors != null) {
            for (String nb : neighbors) {
                Set<String> nbAdj = adj.get(nb);
                if (nbAdj != null) nbAdj.remove(v);
            }
        }
    }

    // ----- queries -----

    Set<String> nodes() {
        return Collections.unmodifiableSet(adj.keySet());
    }

    Set<String> neighbors(String v) {
        Set<String> nb = adj.get(v);
        return nb == null ? Collections.emptySet() : Collections.unmodifiableSet(nb);
    }

    int degree(String v) {
        Set<String> nb = adj.get(v);
        return nb == null ? 0 : nb.size();
    }

    boolean hasEdge(String u, String v) {
        Set<String> nb = adj.get(u);
        return nb != null && nb.contains(v);
    }
}
