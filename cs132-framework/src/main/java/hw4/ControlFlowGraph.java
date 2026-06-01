
package hw4;

import java.util.List;
import java.util.Map;
import java.util.Set;

// Control-flow graph for one Sparrow function.
// nodes[0..n-1] correspond to instructions; nodes[n] is the synthetic
// return node that uses the return identifier.
class ControlFlowGraph {
    final List<ControlFlowNode> nodes;
    final Map<String, Integer> labelMap;  // label name → node id
    final List<String> params;            // formal parameter names in order
    final Set<Integer> reachable;         // node IDs reachable from entry

    ControlFlowGraph(List<ControlFlowNode> nodes,
                     Map<String, Integer> labelMap,
                     List<String> params,
                     Set<Integer> reachable) {
        this.nodes     = nodes;
        this.labelMap  = labelMap;
        this.params    = params;
        this.reachable = reachable;
    }

    ControlFlowNode get(int id) { return nodes.get(id); }
    int size()                  { return nodes.size(); }
}
