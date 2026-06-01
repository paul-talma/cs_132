package hw4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import IR.syntaxtree.Node;
import IR.token.FunctionName;
import IR.token.Label;
import IR.token.Register;
import IR.visitor.DepthFirstVisitor;
import sparrowv.*;

public class Translator extends DepthFirstVisitor {
    // single allocator object for all functions
    FunctionAllocator allocator;
    List<Register> argumentRegisters = new ArrayList<>(Arrays.asList(
            new Register("a2"),
            new Register("a3"),
            new Register("a4"),
            new Register("a5"),
            new Register("a6"),
            new Register("a7")));
    List<Register> calleeSavedRegisters = new ArrayList<>(Arrays.asList(
            new Register("s1"),
            new Register("s2"),
            new Register("s3"),
            new Register("s4"),
            new Register("s5"),
            new Register("s6"),
            new Register("s7"),
            new Register("s8"),
            new Register("s9"),
            new Register("s10"),
            new Register("s11")));
    List<Register> callerSavedRegisters = new ArrayList<>(Arrays.asList(
            new Register("t0"),
            new Register("t1"),
            new Register("t2"),
            new Register("t3")));
    Register t4 = new Register("t4"); // temp registers (never allocated to variables)
    Register t5 = new Register("t5");

    Map<String, String> devirtMap; // funcVar → static func name (per function)
    IntervalList intervalList;
    int originalPos; // mirrors LinearScanVisitor.pos; used for liveness queries at call sites
    FunctionAllocation currentAllocations;
    List<Register> usedCalleeSavedRegs; // callee-saved registers actually allocated in this function
    List<FunctionDecl> functions = new ArrayList<FunctionDecl>();
    List<Instruction> instr;

    Program program;
    boolean isMain; // true while translating the program entry point

    public Translator(FunctionAllocator allocator) {
        this.allocator = allocator;
    }

    public void visit(IR.syntaxtree.Program n) {
        n.f0.accept(this);
        program = new Program(functions);
    }

    public void visit(IR.syntaxtree.FunctionDeclaration n) {
        isMain = functions.isEmpty(); // first function is the program entry point
        // allocation
        currentAllocations = allocator.allocate(n);
        devirtMap = allocator.getDevirtMap();

        // compute which callee-saved registers this function actually uses
        // Use a seen-set to avoid duplicate saves when multiple variables share
        // the same register via non-overlapping intervals.
        usedCalleeSavedRegs = new ArrayList<>();
        java.util.Set<String> seenCallee = new java.util.LinkedHashSet<>();
        for (Home h : currentAllocations.homeOf.values()) {
            if (!h.isRegister())
                continue;
            String regName = h.getReg().toString();
            if (!seenCallee.add(regName))
                continue;
            for (IR.token.Register cs : calleeSavedRegisters) {
                if (cs.toString().equals(regName)) {
                    usedCalleeSavedRegs.add(cs);
                    break;
                }
            }
        }

        // translation
        FunctionName functionName = new FunctionName(n.f1.f0.toString());
        instr = new ArrayList<>();
        originalPos = 0;

        // save only the callee-saved registers actually used in this function
        // (skipped for the program entry point: no caller expects them preserved)
        if (!isMain) {
            for (IR.token.Register r : usedCalleeSavedRegs) {
                String regName = r.toString();
                IR.token.Identifier regStackId = new IR.token.Identifier("callee_saved_" + regName);
                instr.add(new Move_Id_Reg(regStackId, r));
            }
        }

        List<IR.token.Identifier> args = getArgs(n.f3);
        if (Config.ARG_REGS) {
            // Load params from argument registers (a2–a7) for the first 6 params;
            // load from E for any overflow params (7th and beyond).
            for (int i = 0; i < args.size(); i++) {
                IR.token.Identifier param = args.get(i);
                if (!allocator.isLiveAtEntry(param.toString()))
                    continue;
                Home paramHome = currentAllocations.get(param.toString());
                if (paramHome == null)
                    continue;
                if (i < argumentRegisters.size()) {
                    IR.token.Register argReg = argumentRegisters.get(i);
                    if (paramHome.isRegister()) {
                        instr.add(new Move_Reg_Reg(paramHome.getReg(), argReg));
                    } else {
                        instr.add(new Move_Id_Reg(paramHome.getId(), argReg));
                    }
                } else {
                    if (paramHome.isRegister())
                        instr.add(new Move_Reg_Id(paramHome.getReg(), param));
                }
            }
            List<IR.token.Identifier> overflowParams = args.size() > argumentRegisters.size()
                    ? args.subList(argumentRegisters.size(), args.size())
                    : new ArrayList<>();
            Block block = getBlock(n.f5);
            functions.add(new FunctionDecl(functionName, overflowParams, block));
        } else {
            // Original convention: load every register-homed param from its E identifier.
            for (IR.token.Identifier param : args) {
                if (!allocator.isLiveAtEntry(param.toString()))
                    continue;
                Home paramHome = currentAllocations.get(param.toString());
                if (paramHome != null && paramHome.isRegister())
                    instr.add(new Move_Reg_Id(paramHome.getReg(), param));
            }
            Block block = getBlock(n.f5);
            functions.add(new FunctionDecl(functionName, args, block));
        }
    }

    public Block getBlock(IR.syntaxtree.Block b) {
        b.f0.accept(this);

        String returnName = b.f2.f0.toString();
        IR.token.Identifier returnId = new IR.token.Identifier(returnName);

        // spill return value to identifier environment before restoring callee-saved
        // regs
        Home returnHome = currentAllocations.get(returnName);
        if (returnHome != null && returnHome.isRegister()) {
            instr.add(new Move_Id_Reg(returnId, returnHome.getReg()));
        }

        // restore only the callee-saved registers we saved at entry
        // (skipped for the program entry point: nothing to restore to)
        if (!isMain) {
            for (IR.token.Register r : usedCalleeSavedRegs) {
                String regName = r.toString();
                IR.token.Identifier regStackId = new IR.token.Identifier("callee_saved_" + regName);
                instr.add(new Move_Reg_Id(r, regStackId));
            }
        }

        return new Block(instr, returnId);
    }

    // Single reachability gate for every instruction in the function body.
    // Dead instructions are skipped (no code emitted) but originalPos is still
    // incremented so liveness queries at live call sites remain correct.
    public void visit(IR.syntaxtree.Instruction n) {
        if (!allocator.isReachable(originalPos)) {
            originalPos++;
            return;
        }
        n.f0.accept(this);
    }

    public void visit(IR.syntaxtree.LabelWithColon n) {
        String labelName = n.f0.f0.toString();
        instr.add(new LabelInstr(new Label(labelName)));
        originalPos++;
    }

    public void visit(IR.syntaxtree.SetInteger n) {
        String lhsName = n.f0.f0.toString();
        Home lhsHome = currentAllocations.get(lhsName);
        Integer rhs = Integer.parseInt(n.f2.f0.toString());

        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_Integer(lhsHome.getReg(), rhs));
        } else {
            instr.add(new Move_Reg_Integer(t4, rhs));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.SetFuncName n) {
        String lhsName = n.f0.f0.toString();

        // Devirtualized variable: the address is loaded directly at each call site;
        // no register or spill slot is allocated for it.
        if (devirtMap.containsKey(lhsName)) {
            originalPos++;
            return;
        }

        Home lhsHome = currentAllocations.get(lhsName);
        FunctionName funcName = new FunctionName(n.f3.f0.toString());

        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_FuncName(lhsHome.getReg(), funcName));
        } else {
            instr.add(new Move_Reg_FuncName(t4, funcName));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Add n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4, op0Name);
        Register op1reg = materializeUse(op1home, t5, op1Name);
        if (lhsHome.isRegister()) {
            instr.add(new Add(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Add(t4, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Subtract n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4, op0Name);
        Register op1reg = materializeUse(op1home, t5, op1Name);
        if (lhsHome.isRegister()) {
            instr.add(new Subtract(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Subtract(t4, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Multiply n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4, op0Name);
        Register op1reg = materializeUse(op1home, t5, op1Name);
        if (lhsHome.isRegister()) {
            instr.add(new Multiply(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Multiply(t4, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.LessThan n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4, op0Name);
        Register op1reg = materializeUse(op1home, t5, op1Name);
        if (lhsHome.isRegister()) {
            instr.add(new LessThan(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new LessThan(t4, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Load n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f3.f0.toString();
        Integer rhsInt = Integer.parseInt(n.f5.f0.toString());

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4, rhsName);
        if (lhsHome.isRegister()) {
            instr.add(new Load(lhsHome.getReg(), rhsReg, rhsInt));
        } else {
            instr.add(new Load(t5, rhsReg, rhsInt));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Store n) {
        String lhsName = n.f1.f0.toString();
        Integer lhsInt = Integer.parseInt(n.f3.f0.toString());
        String rhsName = n.f6.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4, rhsName);
        if (lhsHome.isRegister()) {
            instr.add(new Store(lhsHome.getReg(), lhsInt, rhsReg));
        } else {
            instr.add(new Move_Reg_Id(t5, lhsHome.getId()));
            instr.add(new Store(t5, lhsInt, rhsReg));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Move n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f2.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        // If coalescing assigned both sides the same register, the copy is a no-op.
        if (lhsHome.isRegister() && rhsHome.isRegister()
                && lhsHome.getReg().toString().equals(rhsHome.getReg().toString())) {
            originalPos++;
            return;
        }

        Register rhsReg = materializeUse(rhsHome, t4, rhsName);
        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_Reg(lhsHome.getReg(), rhsReg));
        } else {
            instr.add(new Move_Reg_Reg(t5, rhsReg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Alloc n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4, rhsName);
        if (lhsHome.isRegister()) {
            instr.add(new Alloc(lhsHome.getReg(), rhsReg));
        } else {
            instr.add(new Alloc(t5, rhsReg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
        originalPos++;
    }

    public void visit(IR.syntaxtree.Print n) {
        String name = n.f2.f0.toString();
        Home home = currentAllocations.get(name);
        Register reg = materializeUse(home, t4, name);
        instr.add(new Print(reg));
        originalPos++;
    }

    public void visit(IR.syntaxtree.ErrorMessage n) {
        String msg = n.f2.f0.toString();
        instr.add(new ErrorMessage(msg));
        originalPos++;
    }

    public void visit(IR.syntaxtree.Goto n) {
        instr.add(new Goto(new Label(n.f1.f0.toString())));
        originalPos++;
    }

    public void visit(IR.syntaxtree.IfGoto n) {
        String condName = n.f1.f0.toString();
        Home condHome = currentAllocations.get(condName);
        Register condReg = materializeUse(condHome, t4, condName);
        instr.add(new IfGoto(condReg, new Label(n.f3.f0.toString())));
        originalPos++;
    }

    public void visit(IR.syntaxtree.Call n) {
        String lhsName = n.f0.f0.toString();
        String funcName = n.f3.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        // Devirt variables have no allocated home; only look up non-devirt ones.
        Home funcHome = devirtMap.containsKey(funcName) ? null : currentAllocations.get(funcName);

        List<IR.token.Identifier> args = getArgs(n.f5);

        // Save live caller-saved registers (t0–t3) to E before the call.
        // Skip saving if the only live variable in the register is lhsName: that
        // variable is defined BY this call and has no pre-call value to preserve.
        List<IR.token.Register> savedCallerRegs = new ArrayList<>();
        for (IR.token.Register r : callerSavedRegisters) {
            String varInReg = varInRegister(r);
            if (varInReg == null || varInReg.equals(lhsName))
                continue;
            IR.token.Identifier stackId = new IR.token.Identifier("caller_saved_" + r.toString());
            instr.add(new Move_Id_Reg(stackId, r));
            savedCallerRegs.add(r);
        }

        // Load args: into a2–a7 when ARG_REGS is on, into E identifiers otherwise.
        List<IR.token.Identifier> overflowArgs;
        if (Config.ARG_REGS) {
            overflowArgs = new ArrayList<>();
            for (int i = 0; i < args.size(); i++) {
                IR.token.Identifier arg = args.get(i);
                Home argHome = currentAllocations.get(arg.toString());
                if (i < argumentRegisters.size()) {
                    IR.token.Register argReg = argumentRegisters.get(i);
                    if (argHome != null && argHome.isRegister()) {
                        instr.add(new Move_Reg_Reg(argReg, argHome.getReg()));
                    } else {
                        instr.add(new Move_Reg_Id(argReg, arg));
                    }
                } else {
                    overflowArgs.add(arg);
                    if (argHome != null && argHome.isRegister())
                        instr.add(new Move_Id_Reg(arg, argHome.getReg()));
                }
            }
        } else {
            // Original convention: materialize all args to E identifiers.
            overflowArgs = new ArrayList<>(args);
            for (IR.token.Identifier arg : args) {
                Home argHome = currentAllocations.get(arg.toString());
                if (argHome != null && argHome.isRegister())
                    instr.add(new Move_Id_Reg(arg, argHome.getReg()));
            }
        }

        // land result in t5 so restores below cannot overwrite it before we move it
        Register funcReg;
        if (devirtMap.containsKey(funcName)) {
            // Devirtualized: load the known function address directly.
            instr.add(new Move_Reg_FuncName(t4, new FunctionName(devirtMap.get(funcName))));
            funcReg = t4;
        } else {
            funcReg = materializeUse(funcHome, t4, funcName);
        }
        instr.add(new Call(t5, funcReg, overflowArgs));

        // restore only the registers we saved; skip the one assigned to the LHS result
        for (IR.token.Register r : savedCallerRegs) {
            if (lhsHome.isRegister() && lhsHome.getReg().toString().equals(r.toString()))
                continue;
            IR.token.Identifier stackId = new IR.token.Identifier("caller_saved_" + r.toString());
            instr.add(new Move_Reg_Id(r, stackId));
        }

        // move call result from t5 into the LHS home
        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_Reg(lhsHome.getReg(), t5));
        } else {
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }

        originalPos++;
    }

    // helpers

    /**
     * Returns the name of the variable currently live in register r at originalPos,
     * or null if no live variable owns r right now.
     * Restricts to variables whose interval contains originalPos to avoid returning
     * a dead variable that happened to be allocated to the same register earlier.
     */
    private String varInRegister(IR.token.Register r) {
        String rStr = r.toString();
        for (Map.Entry<String, Home> entry : currentAllocations.homeOf.entrySet()) {
            Home h = entry.getValue();
            if (!h.isRegister() || !h.getReg().toString().equals(rStr))
                continue;
            String varName = entry.getKey();
            if (allocator.getLiveOutAt(originalPos).contains(varName)) {
                return varName;
            }
        }
        return null;

    }

    Register materializeUse(Home home, Register scratch, String varName) {
        if (home.isRegister())
            return home.getReg();
        Object known = allocator.getRematerialValue(originalPos, varName);
        if (known instanceof Integer) {
            instr.add(new Move_Reg_Integer(scratch, (Integer) known));
            return scratch;
        }
        if (known instanceof String) {
            instr.add(new Move_Reg_FuncName(scratch, new FunctionName((String) known)));
            return scratch;
        }
        instr.add(new Move_Reg_Id(scratch, home.getId()));
        return scratch;
    }

    List<IR.token.Identifier> getArgs(IR.syntaxtree.NodeListOptional n) {
        List<IR.token.Identifier> args = new ArrayList<>();
        if (n.present())
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
                IR.syntaxtree.Identifier id = (IR.syntaxtree.Identifier) e.nextElement();
                args.add(new IR.token.Identifier(id.f0.toString()));
            }
        return args;
    }

    public Program getProgram() {
        return program;
    }

}
