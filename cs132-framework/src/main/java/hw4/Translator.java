package hw4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import IR.syntaxtree.Node;
import IR.token.FunctionName;
import IR.token.Label;
import IR.token.Register;
import IR.visitor.DepthFirstVisitor;
import sparrowv.*;

public class Translator extends DepthFirstVisitor {
    // single allocator object for all functions
    Allocator allocator;
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
    Register t4 = new Register("t4"); // temp registers
    Register t5 = new Register("t5");

    IntervalList intervalList;
    FunctionAllocation currentAllocations;
    List<FunctionDecl> functions = new ArrayList<FunctionDecl>();
    List<Instruction> instr;

    Program program;

    public Translator() {
        List<Register> allFreeRegisters = new ArrayList<>();
        allFreeRegisters.addAll(calleeSavedRegisters);
        allFreeRegisters.addAll(callerSavedRegisters);
        // TODO: revert
        // allocator = new LinearScanAllocator(allFreeRegisters);
        allocator = new TrivialAllocator();
    }

    public void visit(IR.syntaxtree.Program n) {
        n.f0.accept(this);
        program = new Program(functions);
    }

    public void visit(IR.syntaxtree.FunctionDeclaration n) {
        // liveness analysis
        LinearScanVisitor v = new LinearScanVisitor();
        v.visit(n);
        intervalList = v.getIntervalList();

        // allocation
        currentAllocations = allocator.allocate(intervalList);

        // translation
        FunctionName functionName = new FunctionName(n.f1.f0.toString());
        instr = new ArrayList<>();

        List<IR.token.Identifier> args = getArgs(n.f3);

        // load params into their allocated registers
        for (IR.token.Identifier param : args) {
            Home paramHome = currentAllocations.get(param.toString());
            if (paramHome != null && paramHome.isRegister()) {
                instr.add(new Move_Reg_Id(paramHome.getReg(), param));
            }
        }

        Block block = getBlock(n.f5);
        functions.add(new FunctionDecl(functionName, args, block));
    }

    public Block getBlock(IR.syntaxtree.Block b) {
        // save callee-saved registers
        for (IR.token.Register r : calleeSavedRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("callee_saved_" + regName);
            instr.add(new Move_Id_Reg(regStackId, r));
        }
        // retrieve arguments

        b.f0.accept(this);

        String returnName = b.f2.f0.toString();
        IR.token.Identifier returnId = new IR.token.Identifier(returnName);

        // restore callee-saved registers
        for (IR.token.Register r : calleeSavedRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("callee_saved_" + regName);
            instr.add(new Move_Reg_Id(r, regStackId));
        }

        Home returnHome = currentAllocations.get(returnName);
        if (returnHome != null && returnHome.isRegister()) {
            instr.add(new Move_Id_Reg(returnId, returnHome.getReg()));
        }

        return new Block(instr, returnId);
    }

    public void visit(IR.syntaxtree.LabelWithColon n) {
        String labelName = n.f0.f0.toString();
        instr.add(new LabelInstr(new Label(labelName)));
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
    }

    public void visit(IR.syntaxtree.SetFuncName n) {
        String lhsName = n.f0.f0.toString();
        Home lhsHome = currentAllocations.get(lhsName);
        FunctionName funcName = new FunctionName(n.f3.f0.toString());

        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_FuncName(lhsHome.getReg(), funcName));
        } else {
            instr.add(new Move_Reg_FuncName(t4, funcName));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t4));
        }
    }

    public void visit(IR.syntaxtree.Add n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4);
        Register op1reg = materializeUse(op1home, t5);
        if (lhsHome.isRegister()) {
            instr.add(new Add(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Add(op0reg, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), op0reg));
        }
    }

    public void visit(IR.syntaxtree.Subtract n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4);
        Register op1reg = materializeUse(op1home, t5);
        if (lhsHome.isRegister()) {
            instr.add(new Subtract(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Subtract(op0reg, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), op0reg));
        }
    }

    public void visit(IR.syntaxtree.Multiply n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4);
        Register op1reg = materializeUse(op1home, t5);
        if (lhsHome.isRegister()) {
            instr.add(new Multiply(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new Multiply(op0reg, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), op0reg));
        }
    }

    public void visit(IR.syntaxtree.LessThan n) {
        String lhsName = n.f0.f0.toString();
        String op0Name = n.f2.f0.toString();
        String op1Name = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home op0home = currentAllocations.get(op0Name);
        Home op1home = currentAllocations.get(op1Name);

        Register op0reg = materializeUse(op0home, t4);
        Register op1reg = materializeUse(op1home, t5);
        if (lhsHome.isRegister()) {
            instr.add(new LessThan(lhsHome.getReg(), op0reg, op1reg));
        } else {
            instr.add(new LessThan(op0reg, op0reg, op1reg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), op0reg));
        }
    }

    public void visit(IR.syntaxtree.Load n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f3.f0.toString();
        Integer rhsInt = Integer.parseInt(n.f5.f0.toString());

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4);
        if (lhsHome.isRegister()) {
            instr.add(new Load(lhsHome.getReg(), rhsReg, rhsInt));
        } else {
            instr.add(new Load(t5, rhsReg, rhsInt));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
    }

    public void visit(IR.syntaxtree.Store n) {
        String lhsName = n.f1.f0.toString();
        Integer lhsInt = Integer.parseInt(n.f3.f0.toString());
        String rhsName = n.f6.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4);
        if (lhsHome.isRegister()) {
            instr.add(new Store(lhsHome.getReg(), lhsInt, rhsReg));
        } else {
            instr.add(new Move_Reg_Id(t5, lhsHome.getId()));
            instr.add(new Store(t5, lhsInt, rhsReg));
        }
    }

    public void visit(IR.syntaxtree.Move n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f2.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4);
        if (lhsHome.isRegister()) {
            instr.add(new Move_Reg_Reg(lhsHome.getReg(), rhsReg));
        } else {
            instr.add(new Move_Reg_Reg(t5, rhsReg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
    }

    public void visit(IR.syntaxtree.Alloc n) {
        String lhsName = n.f0.f0.toString();
        String rhsName = n.f4.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home rhsHome = currentAllocations.get(rhsName);

        Register rhsReg = materializeUse(rhsHome, t4);
        if (lhsHome.isRegister()) {
            instr.add(new Alloc(lhsHome.getReg(), rhsReg));
        } else {
            instr.add(new Alloc(t5, rhsReg));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }
    }

    public void visit(IR.syntaxtree.Print n) {
        String name = n.f2.f0.toString();

        Home home = currentAllocations.get(name);

        Register reg = materializeUse(home, t4);
        instr.add(new Print(reg));
    }

    public void visit(IR.syntaxtree.ErrorMessage n) {
        String msg = n.f2.f0.toString();
        instr.add(new ErrorMessage(msg));
    }

    public void visit(IR.syntaxtree.Goto n) {
        instr.add(new Goto(new Label(n.f1.f0.toString())));
    }

    public void visit(IR.syntaxtree.IfGoto n) {
        String condName = n.f1.f0.toString();

        Home condHome = currentAllocations.get(condName);

        Register condReg = materializeUse(condHome, t4);
        instr.add(new IfGoto(condReg, new Label(n.f3.f0.toString())));
    }

    public void visit(IR.syntaxtree.Call n) {
        String lhsName = n.f0.f0.toString();
        String funcName = n.f3.f0.toString();

        Home lhsHome = currentAllocations.get(lhsName);
        Home funcHome = currentAllocations.get(funcName);

        // get arg list
        List<IR.token.Identifier> args = getArgs(n.f5);

        // materialize register arguments to stack
        // TODO: update to use argument registers
        for (IR.token.Identifier arg : args) {
            String argName = arg.toString();
            Home argHome = currentAllocations.get(argName);
            if (argHome != null && argHome.isRegister()) {
                instr.add(new Move_Id_Reg(arg, argHome.getReg()));
            }
        }

        // save caller-saved registers
        for (IR.token.Register r : callerSavedRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("caller_saved_" + regName);
            instr.add(new Move_Id_Reg(regStackId, r));
        }
        // save argument registers
        for (IR.token.Register r : argumentRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("argument_saved_" + regName);
            instr.add(new Move_Id_Reg(regStackId, r));
        }

        // call
        Register funcReg = materializeUse(funcHome, t4);
        if (lhsHome.isRegister()) {
            instr.add(new Call(lhsHome.getReg(), funcReg, args));
        } else {
            instr.add(new Call(t5, funcReg, args));
            instr.add(new Move_Id_Reg(lhsHome.getId(), t5));
        }

        // restore caller-saved registers
        for (IR.token.Register r : callerSavedRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("caller_saved_" + regName);
            instr.add(new Move_Reg_Id(r, regStackId));
        }
        // restore argument registers
        for (IR.token.Register r : argumentRegisters) {
            String regName = r.toString();
            IR.token.Identifier regStackId = new IR.token.Identifier("argument_saved_" + regName);
            instr.add(new Move_Reg_Id(r, regStackId));
        }
    }

    // helpers
    Register materializeUse(Home home, Register scratch) {
        if (home.isRegister())
            return home.getReg();
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
        //
        return program;
    }

}

//
