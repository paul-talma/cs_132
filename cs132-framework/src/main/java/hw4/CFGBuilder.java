package hw4;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.syntaxtree.*;

// Builds a ControlFlowGraph from a single Sparrow FunctionDeclaration.
//
// Node numbering:
//   0 .. instrCount-1  — one node per Instruction in the Block
//   instrCount         — synthetic return node (use = {returnVar}, no succs)
//
// Edges:
//   - Every non-Goto instruction has a sequential edge to the next node.
//   - Goto L          → only the target label node (no fall-through).
//   - IfGoto x L      → the target label node AND the next node.
//   - The last real instruction has a fall-through to the return node unless
//     it is a Goto.
class CFGBuilder {

    static ControlFlowGraph build(FunctionDeclaration n) {
        // Collect formal parameter names
        List<String> params = new ArrayList<>();
        if (n.f3.present()) {
            for (Enumeration<Node> e = n.f3.elements(); e.hasMoreElements();) {
                params.add(id((Identifier) e.nextElement()));
            }
        }

        Block block = n.f5;
        String returnVar = block.f2.f0.tokenImage;

        // Collect all Instruction AST nodes in order
        List<IR.syntaxtree.Instruction> instrList = new ArrayList<>();
        if (block.f0.present()) {
            for (Enumeration<Node> e = block.f0.elements(); e.hasMoreElements();) {
                instrList.add((IR.syntaxtree.Instruction) e.nextElement());
            }
        }

        int n_instrs = instrList.size();

        // Create one CFGNode per instruction plus the synthetic return node
        List<ControlFlowNode> nodes = new ArrayList<>(n_instrs + 1);
        for (int i = 0; i < n_instrs; i++) {
            nodes.add(new ControlFlowNode(i, instrList.get(i)));
        }
        ControlFlowNode retNode = new ControlFlowNode(n_instrs, null);
        retNode.use.add(returnVar);
        nodes.add(retNode);

        // First pass: build label map and compute def/use
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < n_instrs; i++) {
            Node choice = instrList.get(i).f0.choice;
            if (choice instanceof LabelWithColon) {
                labelMap.put(((LabelWithColon) choice).f0.f0.tokenImage, i);
            }
            computeDefUse(nodes.get(i), choice);
        }

        // Second pass: wire edges
        for (int i = 0; i < n_instrs; i++) {
            ControlFlowNode node = nodes.get(i);
            Node choice = instrList.get(i).f0.choice;
            ControlFlowNode next = nodes.get(i + 1); // i+1 is valid because retNode is at n_instrs

            if (choice instanceof Goto) {
                String label = ((Goto) choice).f1.f0.tokenImage;
                Integer target = labelMap.get(label);
                if (target != null) addEdge(node, nodes.get(target));
                // no fall-through
            } else if (choice instanceof IfGoto) {
                String label = ((IfGoto) choice).f3.f0.tokenImage;
                Integer target = labelMap.get(label);
                if (target != null) addEdge(node, nodes.get(target));
                addEdge(node, next); // fall-through
            } else {
                addEdge(node, next); // sequential
            }
        }

        // Third pass: compute loop depth via natural-loop detection.
        computeLoopDepths(nodes);

        return new ControlFlowGraph(nodes, labelMap, params);
    }

    // Populate def/use sets for one instruction node.
    private static void computeDefUse(ControlFlowNode node, Node choice) {
        if (choice instanceof SetInteger) {
            node.def.add(((SetInteger) choice).f0.f0.tokenImage);

        } else if (choice instanceof SetFuncName) {
            node.def.add(((SetFuncName) choice).f0.f0.tokenImage);

        } else if (choice instanceof Add) {
            Add a = (Add) choice;
            node.def.add(a.f0.f0.tokenImage);
            node.use.add(a.f2.f0.tokenImage);
            node.use.add(a.f4.f0.tokenImage);

        } else if (choice instanceof Subtract) {
            Subtract s = (Subtract) choice;
            node.def.add(s.f0.f0.tokenImage);
            node.use.add(s.f2.f0.tokenImage);
            node.use.add(s.f4.f0.tokenImage);

        } else if (choice instanceof Multiply) {
            Multiply m = (Multiply) choice;
            node.def.add(m.f0.f0.tokenImage);
            node.use.add(m.f2.f0.tokenImage);
            node.use.add(m.f4.f0.tokenImage);

        } else if (choice instanceof LessThan) {
            LessThan lt = (LessThan) choice;
            node.def.add(lt.f0.f0.tokenImage);
            node.use.add(lt.f2.f0.tokenImage);
            node.use.add(lt.f4.f0.tokenImage);

        } else if (choice instanceof Load) {
            Load l = (Load) choice;
            node.def.add(l.f0.f0.tokenImage);
            node.use.add(l.f3.f0.tokenImage);

        } else if (choice instanceof Store) {
            Store s = (Store) choice;
            node.use.add(s.f1.f0.tokenImage);
            node.use.add(s.f6.f0.tokenImage);

        } else if (choice instanceof Move) {
            Move mv = (Move) choice;
            node.def.add(mv.f0.f0.tokenImage);
            node.use.add(mv.f2.f0.tokenImage);

        } else if (choice instanceof Alloc) {
            Alloc al = (Alloc) choice;
            node.def.add(al.f0.f0.tokenImage);
            node.use.add(al.f4.f0.tokenImage);

        } else if (choice instanceof Print) {
            node.use.add(((Print) choice).f2.f0.tokenImage);

        } else if (choice instanceof IfGoto) {
            node.use.add(((IfGoto) choice).f1.f0.tokenImage);

        } else if (choice instanceof Call) {
            Call c = (Call) choice;
            node.def.add(c.f0.f0.tokenImage);
            node.use.add(c.f3.f0.tokenImage);
            if (c.f5.present()) {
                for (Enumeration<Node> e = c.f5.elements(); e.hasMoreElements();) {
                    node.use.add(id((Identifier) e.nextElement()));
                }
            }
            node.isCall = true;
        }
        // LabelWithColon, ErrorMessage, Goto: empty def/use
    }


    // For each back edge (src → header, where header.id ≤ src.id), find the
    // natural loop body via backward BFS from src up to header, and increment
    // every body node's loopDepth.
    private static void computeLoopDepths(List<ControlFlowNode> nodes) {
        for (ControlFlowNode src : nodes) {
            for (ControlFlowNode tgt : src.succs) {
                if (tgt.id <= src.id) {
                    markNaturalLoop(tgt, src);
                }
            }
        }
    }

    private static void markNaturalLoop(ControlFlowNode header, ControlFlowNode tail) {
        Set<ControlFlowNode> body = new HashSet<>();
        body.add(header);
        Deque<ControlFlowNode> worklist = new ArrayDeque<>();
        if (!tail.equals(header)) {
            body.add(tail);
            worklist.add(tail);
        }
        while (!worklist.isEmpty()) {
            ControlFlowNode n = worklist.poll();
            for (ControlFlowNode pred : n.preds) {
                if (!body.contains(pred)) {
                    body.add(pred);
                    worklist.add(pred);
                }
            }
        }
        for (ControlFlowNode n : body) {
            n.loopDepth++;
        }
    }

    private static void addEdge(ControlFlowNode from, ControlFlowNode to) {
        from.succs.add(to);
        to.preds.add(from);
    }

    private static String id(Identifier n) {
        return n.f0.tokenImage;
    }
}
