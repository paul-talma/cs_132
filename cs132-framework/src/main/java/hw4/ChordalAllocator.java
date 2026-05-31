package hw4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
//   4. Coalescing           (coalesceAll)  — merges copy-related variables
//   5. MCS ordering         (maxCardinalitySearch)
//   6. Greedy coloring      (greedyColor)  — respects pre-colors from coalescing
//   7. Spill uncolored vars into their identifier environment
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
    private Map<String, Integer> spillCost;          // varName → spill cost
    private Map<String, String>  devirtMap;            // funcVar → static func name

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

        // Compute spill cost: weight = 10^loopDepth for each instruction.
        // Each variable accumulates the weights of all instructions that define or use it.
        spillCost = new HashMap<>();
        for (ControlFlowNode node : cfg.nodes) {
            int weight = 1;
            for (int i = 0; i < node.loopDepth; i++) weight *= 10;
            for (String v : node.def) spillCost.merge(v, weight, Integer::sum);
            for (String v : node.use) spillCost.merge(v, weight, Integer::sum);
        }

        // Identify function-pointer variables that are always set to the same
        // static function name and only used as callees → they need no register.
        buildDevirtMap();

        // Build interference graph, then remove devirt variables: they need no
        // register allocation and should not consume a color.
        InterferenceGraph ig = InterferenceGraph.build(cfg);
        for (String v : devirtMap.keySet()) ig.removeNode(v);

        // Variables live-out at any call site must survive the call → prefer
        // callee-saved registers so no explicit save/restore is needed.
        Set<String> crossesCall = new HashSet<>();
        for (Set<String> liveOut : liveOutAtCall.values()) {
            crossesCall.addAll(liveOut);
        }

        // Capture all variables before coalescing removes merged nodes from IG.
        Set<String> allVars = new LinkedHashSet<>(ig.nodes());

        // Union-find: parent[v] = v initially; after coalescing parent[y] = x
        // means y was merged into x (x is the representative).
        Map<String, String> parent = new HashMap<>();
        for (String v : allVars) parent.put(v, v);

        // Pre-colors assigned by coalescing (representative → register name).
        Map<String, String> preColor = new HashMap<>();
        coalesceAll(ig, crossesCall, preColor, parent);

        // MCS ordering then greedy coloring (pre-colored nodes are already fixed)
        List<String> mcsOrder = maxCardinalitySearch(ig);
        verifyChordalOrdering(mcsOrder, ig, n.f1.f0.toString());
        Map<String, String> coloring = greedyColor(mcsOrder, ig, crossesCall, preColor);

        // Build homeOf for ALL original vars via union-find.
        // Merged vars share the representative's register; spilled vars use their
        // own identifier so spill slots remain distinct.
        Map<String, Home> homeOf = new LinkedHashMap<>();
        for (String var : allVars) {
            if (devirtMap.containsKey(var)) continue; // no home — handled at call sites
            String rep = find(parent, var);
            String regName = coloring.get(rep);
            if (regName != null) {
                homeOf.put(var, new Home(new Register(regName)));
            } else {
                homeOf.put(var, new Home(new Identifier(var)));
            }
        }

        return new FunctionAllocation(homeOf);
    }

    // Returns the devirtualization map: funcVar → static function name.
    // Variables in this map are excluded from register allocation; the Translator
    // loads their target address directly with Move_Reg_FuncName at each call site.
    public Map<String, String> getDevirtMap() {
        return devirtMap != null ? devirtMap : Collections.emptyMap();
    }

    // Returns true if varName is live at function entry (i.e., used somewhere
    // in the body and needs to be loaded in the prologue).
    public boolean isLiveAtEntry(String varName) {
        if (cfg == null || cfg.nodes.isEmpty())
            return false;
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
            // Pick the unprocessed node with the highest weight.
            // Tie-break by lowest spill cost: low-cost nodes are picked first by MCS,
            // so they appear earliest in mcsOrder and are processed last by greedy,
            // making them the most likely candidates to be spilled.
            String best = null;
            int bestW = -1;
            for (String v : allVars) {
                if (!processed.contains(v)) {
                    int w = weight.get(v);
                    int cost = spillCost.getOrDefault(v, 1);
                    int bestCost = best == null ? Integer.MAX_VALUE : spillCost.getOrDefault(best, 1);
                    if (w > bestW || (w == bestW && cost < bestCost)) {
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
    // (left uncolored). Pre-colored nodes (from coalescing) are treated as
    // already assigned and skipped.
    private Map<String, String> greedyColor(List<String> mcsOrder,
            InterferenceGraph ig,
            Set<String> crossesCall,
            Map<String, String> preColor) {
        // Seed with pre-colors so neighbor checks see them.
        Map<String, String> color = new HashMap<>(preColor);

        // Process in reverse MCS order
        List<String> reversed = new ArrayList<>(mcsOrder);
        Collections.reverse(reversed);

        for (String v : reversed) {
            if (color.containsKey(v)) continue; // already colored by coalescing

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
            if (chosen == null) {
                // All registers taken. Try to steal from the colored neighbor with the
                // lowest spill cost whose color is not shared by any other neighbor of v.
                // Only steal if that neighbor is cheaper to spill than v itself.
                String stealNode = null;
                String stealColor = null;
                int threshold = spillCost.getOrDefault(v, 1);

                for (String nb : ig.neighbors(v)) {
                    if (preColor.containsKey(nb)) continue; // never steal pre-colored nodes
                    String c = color.get(nb);
                    if (c == null) continue;
                    // Only steal if c is unique among v's neighbors (freeing it gives v a slot).
                    boolean unique = true;
                    for (String other : ig.neighbors(v)) {
                        if (!other.equals(nb) && c.equals(color.get(other))) {
                            unique = false;
                            break;
                        }
                    }
                    if (unique) {
                        int nbCost = spillCost.getOrDefault(nb, 1);
                        if (nbCost < threshold) {
                            threshold = nbCost;
                            stealNode = nb;
                            stealColor = c;
                        }
                    }
                }

                if (stealNode != null) {
                    color.remove(stealNode); // stealNode is now spilled
                    chosen = stealColor;
                }
            }
            // If chosen == null the variable is spilled (left uncolored)
            if (chosen != null)
                color.put(v, chosen);
        }

        return color;
    }

    // Verifies that the reverse of mcsOrder is a perfect elimination ordering
    // (PEO),
    // which holds iff the interference graph is chordal. For each node v at
    // position i
    // in the PEO, its neighbors at later positions must form a clique. Prints to
    // stderr
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
                if (pos.get(nb) > i)
                    later.add(nb);
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

    // ----- Devirtualization analysis -----
    //
    // A variable qualifies if:
    //   (a) every definition is a SetFuncName to the same function name, and
    //   (b) every use is as the callee of a Call instruction (never as an
    //       argument, operand, or return value).
    private void buildDevirtMap() {
        devirtMap = new HashMap<>();

        Map<String, Set<String>> defFuncs     = new HashMap<>(); // var → set of @F names seen
        Set<String> hasNonFuncDef             = new HashSet<>(); // defined by non-SetFuncName
        Set<String> hasNonCalleeUse           = new HashSet<>(); // used outside callee position

        for (ControlFlowNode node : cfg.nodes) {
            if (node.instr == null) {
                // Synthetic return node: the return variable is a non-callee use.
                for (String v : node.use) hasNonCalleeUse.add(v);
                continue;
            }
            Object choice = node.instr.f0.choice;

            if (choice instanceof IR.syntaxtree.SetFuncName) {
                IR.syntaxtree.SetFuncName sfn = (IR.syntaxtree.SetFuncName) choice;
                String lhs  = sfn.f0.f0.tokenImage;
                String func = sfn.f3.f0.tokenImage;
                defFuncs.computeIfAbsent(lhs, k -> new HashSet<>()).add(func);

            } else if (choice instanceof IR.syntaxtree.Call) {
                IR.syntaxtree.Call c = (IR.syntaxtree.Call) choice;
                // lhs is defined by Call (not a SetFuncName definition).
                hasNonFuncDef.add(c.f0.f0.tokenImage);
                // callee position is a legal use for devirt candidates — skip it.
                // Args are non-callee uses.
                if (c.f5.present()) {
                    for (java.util.Enumeration<IR.syntaxtree.Node> e = c.f5.elements();
                            e.hasMoreElements();) {
                        hasNonCalleeUse.add(
                            ((IR.syntaxtree.Identifier) e.nextElement()).f0.tokenImage);
                    }
                }
            } else {
                // All other instructions: defs are non-SetFuncName, uses are non-callee.
                for (String v : node.def) hasNonFuncDef.add(v);
                for (String v : node.use) hasNonCalleeUse.add(v);
            }
        }

        for (Map.Entry<String, Set<String>> entry : defFuncs.entrySet()) {
            String var  = entry.getKey();
            Set<String> funcs = entry.getValue();
            if (funcs.size() == 1
                    && !hasNonFuncDef.contains(var)
                    && !hasNonCalleeUse.contains(var)) {
                devirtMap.put(var, funcs.iterator().next());
            }
        }
    }

    // ----- Coalescing -----
    //
    // For each copy instruction x := y, if x and y do not interfere and there
    // exists a register c not used by any already-colored neighbor of x or y,
    // merge them into a single node (keeping x as representative) with color c.
    // y is removed from the graph; x absorbs y's edges and becomes the merged node.
    private void coalesceAll(InterferenceGraph ig,
                              Set<String> crossesCall,
                              Map<String, String> preColor,
                              Map<String, String> parent) {
        // Collect copy pairs (lhs, rhs) from Move instructions in the CFG.
        List<String[]> copies = new ArrayList<>();
        for (ControlFlowNode node : cfg.nodes) {
            if (node.instr == null) continue;
            Object choice = node.instr.f0.choice;
            if (choice instanceof IR.syntaxtree.Move) {
                IR.syntaxtree.Move m = (IR.syntaxtree.Move) choice;
                copies.add(new String[]{ m.f0.f0.tokenImage, m.f2.f0.tokenImage });
            }
        }

        for (String[] pair : copies) {
            String x = find(parent, pair[0]);
            String y = find(parent, pair[1]);

            if (x.equals(y)) continue;             // already the same node
            if (!ig.nodes().contains(x)) continue;
            if (!ig.nodes().contains(y)) continue;
            if (ig.hasEdge(x, y)) continue;        // interfere — cannot coalesce

            // Sx ∪ Sy: colors of already-colored neighbors of x and y.
            Set<String> usedColors = new HashSet<>();
            for (String nb : ig.neighbors(x))
                if (preColor.containsKey(nb)) usedColors.add(preColor.get(nb));
            for (String nb : ig.neighbors(y))
                if (preColor.containsKey(nb)) usedColors.add(preColor.get(nb));

            // Determine color c for the merged node.
            String px = preColor.get(x);
            String py = preColor.get(y);
            String chosen;
            if (px != null && py != null) {
                if (!px.equals(py)) continue;     // conflicting pre-colors
                chosen = px;
            } else if (px != null) {
                if (usedColors.contains(px)) continue;
                chosen = px;
            } else if (py != null) {
                if (usedColors.contains(py)) continue;
                chosen = py;
            } else {
                boolean crossCall = crossesCall.contains(x) || crossesCall.contains(y);
                if (crossCall) {
                    chosen = pickFrom(CALLEE_REG_NAMES, usedColors);
                    if (chosen == null) chosen = pickFrom(CALLER_REG_NAMES, usedColors);
                } else {
                    chosen = pickFrom(CALLER_REG_NAMES, usedColors);
                    if (chosen == null) chosen = pickFrom(CALLEE_REG_NAMES, usedColors);
                }
                if (chosen == null) continue;     // no register available
            }

            // Merge: add all of y's edges to x, then remove y.
            // x keeps its existing edges; the merged node is x.
            for (String nb : new ArrayList<>(ig.neighbors(y))) {
                if (!nb.equals(x)) ig.addEdge(x, nb);
            }
            ig.removeNode(y);

            parent.put(y, x);
            preColor.put(x, chosen);
            if (crossesCall.contains(y)) crossesCall.add(x);
            // Accumulate y's cost into x so the merged node's cost is correct.
            spillCost.merge(x, spillCost.getOrDefault(y, 0), Integer::sum);
        }
    }

    // Path-compressed union-find: returns the root representative of v.
    private String find(Map<String, String> parent, String v) {
        while (parent.containsKey(v) && !parent.get(v).equals(v))
            v = parent.get(v);
        return v;
    }

    private static String pickFrom(List<String> pool, Set<String> used) {
        for (String r : pool) {
            if (!used.contains(r))
                return r;
        }
        return null;
    }
}
