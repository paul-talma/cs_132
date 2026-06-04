package hw5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// import IR.syntaxtree.*;
import sparrowv.*;

public class SV2VCompiler extends sparrowv.visitor.DepthFirst {
    List<String> program = new ArrayList<>();
    List<String> instructions;
    String currentFunctionName;
    Map<String, Integer> offsets;
    Set<String> calleeSavedRegisters = new HashSet<>(Arrays.asList(
            "s1",
            "s2",
            "s3",
            "s4",
            "s5",
            "s6",
            "s7",
            "s8",
            "s9",
            "s10",
            "s11"));
    Set<String> usedCalleeSavedRegs;

    public void visit(Program n) {
        // globals
        program.add(".equiv @print_int, 1\n");
        program.add(".equiv @print_string, 4\n");
        program.add(".equiv @print_char, 11\n");
        program.add(".equiv @exit, 10\n\n");

        program.add(".text\n");
        program.add("    jal Main\n");
        program.add("    li a0, @exit\n");
        program.add("    ecall\n\n");

        for (FunctionDecl fd : n.funDecls) {
            fd.accept(this);
            program.addAll(instructions);
        }
    }

    public void visit(FunctionDecl n) {
        instructions = new ArrayList<String>();
        currentFunctionName = n.functionName.toString();
        instructions.add(currentFunctionName + ":\n");

        usedCalleeSavedRegs = new HashSet<String>();
        offsets = getOffsets(n.block);
        int numAllocations = offsets.size() + usedCalleeSavedRegs.size();
        int allocationSize = (numAllocations + 2) * 4;

        // assign stack slots for callee-saved regs after the identifier slots
        Map<String, Integer> calleeSavedOffsets = new HashMap<>();
        int csOffset = 12 + offsets.size() * 4;
        for (String reg : usedCalleeSavedRegs) {
            calleeSavedOffsets.put(reg, csOffset);
            csOffset += 4;
        }

        // prologue
        instructions.add("    sw fp, -8(sp)\n");
        instructions.add("    lw fp, 0(sp)\n");
        instructions.add(String.format("    li t0, %d\n", allocationSize));
        instructions.add("    sub sp, sp, t0\n");
        instructions.add("    sw ra, -4(fp)\n");

        // save callee-saved registers
        for (Map.Entry<String, Integer> e : calleeSavedOffsets.entrySet()) {
            instructions.add("    sw " + e.getKey() + ", " + e.getValue() + "(fp)\n");
        }

        String returnVar = n.block.return_id.toString();
        n.block.accept(this);
        // TODO: need to handle return id?

        // epilogue
        // TODO: load return val?
        // restore callee-saved registers
        for (Map.Entry<String, Integer> e : calleeSavedOffsets.entrySet()) {
            instructions.add("    lw " + e.getKey() + ", " + e.getValue() + "(fp)\n");
        }
        Integer returnOffset = offsets.get(returnVar);
        if (returnOffset == null)
            throw new RuntimeException(
                    "return identifier '" + returnVar + "' has no stack slot in " + currentFunctionName);
        instructions.add("    lw a0, " + returnOffset + "(fp)\n");
        instructions.add("    lw ra, -4(fp)\n");
        instructions.add("    lw fp, -8(fp)\n");
        instructions.add(String.format("    addi sp, sp, %d\n", allocationSize));
        instructions.add("    jr ra\n");
    }

    public void visit(LabelInstr n) {
        String labelName = n.toString();
        instructions.add(currentFunctionName + "_" + labelName + ":\n");
    }

    public void visit(Move_Reg_Integer n) {
        String lhsName = n.lhs.toString();
        instructions.add("    li " + lhsName + ", " + n.rhs + "\n"); // li r, i
    }

    public void visit(Move_Reg_FuncName n) {
        String lhsName = n.lhs.toString();
        String funcName = n.rhs.toString();
        instructions.add("    la " + lhsName + ", " + funcName + "\n"); // la r, Fun
    }

    public void visit(Add n) {
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        instructions.add("    add " + lhs + ", " + arg1 + ", " + arg2 + "\n"); // add r0, r1, r2
    }

    public void visit(Subtract n) {
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        instructions.add("    sub " + lhs + ", " + arg1 + ", " + arg2 + "\n"); // sub r0, r1, r2
    }

    public void visit(Multiply n) {
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        instructions.add("    mul " + lhs + ", " + arg1 + ", " + arg2 + "\n"); // mul r0, r1, r2
    }

    public void visit(LessThan n) {
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        instructions.add("    slt " + lhs + ", " + arg1 + ", " + arg2 + "\n"); // slt r0, r1, r2
    }

    public void visit(Load n) {
        String lhs = n.lhs.toString();
        String base = n.base.toString();
        int offset = n.offset;
        instructions.add("    lw " + lhs + ", " + offset + "(" + base + ")\n"); // lw r0, i(r2)
    }

    public void visit(Store n) {
        String base = n.base.toString();
        int offset = n.offset;
        String rhs = n.rhs.toString();
        instructions.add("    sw " + rhs + ", " + offset + "(" + base + ")\n"); // sw r0, i(r2)
    }

    public void visit(Move_Reg_Reg n) {
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();
        instructions.add("    mv " + lhs + ", " + rhs + "\n");
    }

    public void visit(Move_Id_Reg n) {
        String rhs = n.rhs.toString();
        String lhs = n.lhs.toString();
        int offset = offsets.get(lhs);
        instructions.add("    sw " + rhs + ", " + offset + "(fp)\n");
    }

    public void visit(Move_Reg_Id n) {
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();
        int offset = offsets.get(rhs);
        instructions.add("    lw " + lhs + ", " + offset + "(fp)\n");
    }

    public void visit(Alloc n) {
        String lhs = n.lhs.toString();
        String size = n.size.toString();
        instructions.add("    mv a0, " + size + "\n");
        instructions.add("    jal alloc\n");
        instructions.add("    mv " + lhs + ", a0\n");
    }

    public void visit(Print n) {
        String contentReg = n.content.toString();
        instructions.add("    mv a1, " + contentReg + "\n");
        instructions.add("    li a0, @print_int\n");
        instructions.add("    ecall\n");
        instructions.add("    li a1, 10\n");
        instructions.add("    li a0, @print_char\n");
        instructions.add("    ecall\n");
    }

    public void visit(ErrorMessage n) {
        String msg = n.msg;
        instructions.add("    la a1, " + msg + "\n");
        instructions.add("    li a0, @print_string\n");
        instructions.add("    ecall\n");
        instructions.add("    li a1, 10\n");
        instructions.add("    li a0, @print_char\n");
        instructions.add("    ecall\n");
        instructions.add("    li a0, @exit\n");
        instructions.add("    ecall");
    }

    public void visit(Goto n) {
        String label = n.label.toString();
        instructions.add("    jal " + currentFunctionName + "_" + label + "\n");
    }

    public void visit(IfGoto n) {
        String cond = n.condition.toString();
        String label = n.label.toString();

        instructions.add("    bnez t0, " + currentFunctionName + "_" + label + "no_long_jump\n");
        instructions.add("    jal " + currentFunctionName + "_" + label + "\n");
        instructions.add("    " + currentFunctionName + "_" + label + "no_long_jump:\n");
    }

    public void visit(Call n) {

    }

    /*
     * helpers
     */

    void checkCalleeSaved(String reg) {
        if (calleeSavedRegisters.contains(reg))
            usedCalleeSavedRegs.add(reg);
    }

    // offset(var) = x if the value of var is at x(fp)
    // also records def'ed callee-saved regs
    Map<String, Integer> getOffsets(Block b) {
        Set<String> vars = new HashSet<>();
        for (Instruction i : b.instructions) {
            if (i instanceof Move_Id_Reg) {
                vars.add(((Move_Id_Reg) i).lhs.toString());
            } else if (i instanceof Move_Reg_Id) {
                vars.add(((Move_Reg_Id) i).rhs.toString());
                checkCalleeSaved(((Move_Reg_Id) i).lhs.toString());
            } else if (i instanceof Move_Reg_Integer) {
                checkCalleeSaved(((Move_Reg_Integer) i).lhs.toString());
            } else if (i instanceof Move_Reg_Reg) {
                checkCalleeSaved(((Move_Reg_Reg) i).lhs.toString());
            } else if (i instanceof Move_Reg_FuncName) {
                checkCalleeSaved(((Move_Reg_FuncName) i).lhs.toString());
            }
        }

        Map<String, Integer> offsets = new HashMap<>();
        int offset = -12;
        for (String name : vars) {
            offsets.put(name, offset);
            offset -= 4;
        }
        return offsets;
    }

    public List<String> getProgram() {
        return program;
    }
}
