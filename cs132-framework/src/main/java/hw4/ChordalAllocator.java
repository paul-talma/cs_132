package hw4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.syntaxtree.FunctionDeclaration;
import IR.token.Identifier;
import IR.token.Register;

// Chordal-graph register allocator.
//
// Pipeline per function:
//   1. Build CFG            (CFGBuilder)
//   2. Liveness analysis    (LivenessAnalyzer)
//   3. Interference graph   (InterferenceGraph.build)
//   4. MCS ordering         (maxCardinalitySearch)
//   5. Greedy coloring      (greedyColor)
//   6. Spill uncolored vars into their identifier environment
//
// Register pools:
//   calleeRegs  s1–s11  preferred for variables live across calls
//   callerRegs  t0–t3   preferred for variables that never cross a call
//
// Argument registers a2–a7 are NOT used for variable allocation: they are
// clobbered by any callee that loads its own parameters (via `r = id`) into
// them.  Variables (including parameters) receive s1–s11 or t0–t3 just like
// in the linear-scan allocator.  Parameters are loaded from the identifier
// environment into their allocated register in the function prologue.
//
// The allocator also records, per call-site instruction index, which variables
// are live-out at that point.  Translator uses this for caller-save logic.
public class ChordalAllocator {

    static final List<String> CALLEE_REG_NAMES = Arrays.asList(
            "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11");
    static final List<String> CALLER_REG_NAMES = Arrays.asList(
            "t0", "t1", "t2", "t3");

    // Per-function state, reset on each allocate() call
    private ControlFlowGraph cfg;
    private Map<Integer, Set<String>> liveOutAtCall; // instrIdx → liveOut vars

    // ----- public API -----

    public FunctionAllocation allocate(FunctionDeclaration n) {
        cfg = CFGBuilder.build(n);
        LivenessAnalyzer.analyze(cfg);

        // Collect call-site liveness before building the interference graph
        liveOutAtCall = new HashMap<>();
        for (ControlFlowNode node : cfg.nodes) {
            if (node.isCall) {
                liveOutAtCall.put(node.id, new HashSet<>(node.liveOut));
            }
        }

        // Build interference graph (no parameter pre-coloring)
        InterferenceGraph ig = InterferenceGraph.build(cfg);

        // Variables live-out at any call site must survive the call → prefer
        // callee-saved registers so no explicit save/restore is needed.
        Set<String> crossesCall = new HashSet<>();
        for (Set<String> liveOut : liveOutAtCall.values()) {
            crossesCall.addAll(liveOut);
        }

        // MCS ordering then greedy coloring
        List<String> mcsOrder = maxCardinalitySearch(ig);
        verifyChordalOrdering(mcsOrder, ig, n.f1.f0.toString());
        Map<String, String> coloring = greedyColor(mcsOrder, ig, crossesCall);

        // Convert coloring to FunctionAllocation (spill uncolored vars)
        Map<String, Home> homeOf = new LinkedHashMap<>();
        for (String var : ig.nodes()) {
            String regName = coloring.get(var);
            if (regName != null) {
                homeOf.put(var, new Home(new Register(regName)));
            } else {
                homeOf.put(var, new Home(new Identifier(var)));
            }
        }

        return new FunctionAllocation(homeOf);
    }

    // Returns true if varName is live at function entry (i.e., used somewhere
    // in the body and needs to be loaded in the prologue).
    public boolean isLiveAtEntry(String varName) {
        if (cfg == null || cfg.nodes.isEmpty()) return false;
        return cfg.nodes.get(0).liveIn.contains(varName);
    }

    // Returns the set of variables live after the call at instruction index
    // instrIdx, or an empty set if instrIdx is not a call site.
    public Set<String> getLiveOutAt(int instrIdx) {
        Set<String> s = liveOutAtCall.get(instrIdx);
        return s != null ? s : Collections.emptySet();
    }

    // ----- Maximum Cardinality Search -----
    //
    // Produces an ordering of all nodes such that greedy coloring in reverse
    // order is optimal for chordal graphs.
    private List<String> maxCardinalitySearch(InterferenceGraph ig) {
        Set<String> allVars = new HashSet<>(ig.nodes());
        Map<String, Integer> weight = new HashMap<>();
        for (String v : allVars)
            weight.put(v, 0);

        List<String> order = new ArrayList<>(allVars.size());
        Set<String> processed = new HashSet<>();

        while (processed.size() < allVars.size()) {
            // Pick the unprocessed node with the highest weight (ties: any order)
            String best = null;
            int bestW = -1;
            for (String v : allVars) {
                if (!processed.contains(v)) {
                    int w = weight.get(v);
                    if (w > bestW) {
                        bestW = w;
                        best = v;
                    }
                }
            }
            order.add(best);
            processed.add(best);
            // Increment weight of unprocessed neighbors
            for (String nb : ig.neighbors(best)) {
                if (!processed.contains(nb)) {
                    weight.put(nb, weight.get(nb) + 1);
                }
            }
        }

        return order;
    }

    // ----- Greedy Coloring in reverse MCS order -----
    //
    // Colors are register names. Nodes that cannot be colored are spilled
    // (left uncolored).
    private Map<String, String> greedyColor(List<String> mcsOrder,
            InterferenceGraph ig,
            Set<String> crossesCall) {
        Map<String, String> color = new HashMap<>();

        // Process in reverse MCS order
        List<String> reversed = new ArrayList<>(mcsOrder);
        Collections.reverse(reversed);

        for (String v : reversed) {

            // Gather colors used by already-colored neighbors
            Set<String> usedColors = new HashSet<>();
            for (String nb : ig.neighbors(v)) {
                String c = color.get(nb);
                if (c != null)
                    usedColors.add(c);
            }

            // Choose register: prefer callee if crosses call, caller otherwise
            String chosen = null;
            if (crossesCall.contains(v)) {
                chosen = pickFrom(CALLEE_REG_NAMES, usedColors);
                if (chosen == null)
                    chosen = pickFrom(CALLER_REG_NAMES, usedColors);
            } else {
                chosen = pickFrom(CALLER_REG_NAMES, usedColors);
                if (chosen == null)
                    chosen = pickFrom(CALLEE_REG_NAMES, usedColors);
            }
            // If chosen == null the variable is spilled (left uncolored)
            if (chosen != null)
                color.put(v, chosen);
        }

        return color;
    }


    // Verifies that the reverse of mcsOrder is a perfect elimination ordering (PEO),
    // which holds iff the interference graph is chordal. For each node v at position i
    // in the PEO, its neighbors at later positions must form a clique. Prints to stderr
    // if the graph is not chordal (stdout is reserved for the Sparrow-V output).
    private void verifyChordalOrdering(List<String> mcsOrder, InterferenceGraph ig, String funcName) {
        List<String> peo = new ArrayList<>(mcsOrder);
        Collections.reverse(peo);

        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < peo.size(); i++)
            pos.put(peo.get(i), i);

        for (int i = 0; i < peo.size(); i++) {
            String v = peo.get(i);
            List<String> later = new ArrayList<>();
            for (String nb : ig.neighbors(v))
                if (pos.get(nb) > i) later.add(nb);
            for (int a = 0; a < later.size(); a++)
                for (int b = a + 1; b < later.size(); b++)
                    if (!ig.hasEdge(later.get(a), later.get(b))) {
                        System.err.println("NOT CHORDAL: " + funcName +
                            ": " + v + " not simplicial (missing edge " +
                            later.get(a) + " -- " + later.get(b) + ")");
                        return;
                    }
        }
    }

    private static String pickFrom(List<String> pool, Set<String> used) {
        for (String r : pool) {
            if (!used.contains(r))
                return r;
        }
        return null;
    }
}
