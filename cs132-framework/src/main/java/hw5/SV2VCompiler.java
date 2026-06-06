package hw5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.token.Identifier;
// import IR.syntaxtree.*;
import sparrowv.*;

public class SV2VCompiler extends sparrowv.visitor.DepthFirst {
    List<String> program = new ArrayList<>();
    List<String> instructions;
    String currentFunctionName;
    Map<String, Integer> offsets;
    int labelCounter = 0;

    public void visit(Program n) {
        String entryFunctionName = getEntryFunction(n.funDecls);
        addPrelude(entryFunctionName);

        for (FunctionDecl fd : n.funDecls) {
            fd.accept(this);
            program.addAll(instructions);
        }

        addPostlude();
    }

    String mangle(String name) {
        return "_" + name;
    }

    public void visit(FunctionDecl n) {
        instructions = new ArrayList<String>();
        currentFunctionName = mangle(n.functionName.toString());
        instructions.add(currentFunctionName + ":\n");

        List<Identifier> params = n.formalParameters;
        List<String> locals = getLocals(n.block, params);
        offsets = getOffsets(params, locals);

        int numAllocations = locals.size() + 2; // locals + ra + fp
        int allocationSize = numAllocations * 4;

        // prologue
        instructions.add("    sw fp, -8(sp)\n");
        instructions.add("    mv fp, sp\n");
        instructions.add(String.format("    li t0, %d\n", allocationSize));
        instructions.add("    sub sp, sp, t0\n");
        instructions.add("    sw ra, -4(fp)\n");

        n.block.accept(this);

        // epilogue — load return val
        String returnVar = n.block.return_id.toString();
        Integer returnOffset = offsets.get(returnVar);
        if (returnOffset == null)
            throw new RuntimeException(
                    "return identifier '" + returnVar + "' has no stack slot in " + currentFunctionName);
        // instructions.add("////// LOAD RETURN VAL\n");
        instructions.add("    lw a0, " + returnOffset + "(fp)\n");

        // restore return address, frame pointer
        // instructions.add("////// RESTORE RA, FP\n");
        instructions.add("    lw ra, -4(fp)\n");
        instructions.add("    lw fp, -8(fp)\n");
        // deallocate stack
        // instructions.add("////// DEALLOCATE STACK\n");
        instructions.add(String.format("    addi sp, sp, %d\n", allocationSize));
        // return
        instructions.add("    jr ra\n\n");
    }

    public void visit(LabelInstr n) {
        String labelName = n.toString();
        instructions.add(currentFunctionName + "_" + labelName + "\n");
    }

    public void visit(Move_Reg_Integer n) {
        String lhsName = n.lhs.toString();
        instructions.add("    li " + lhsName + ", " + n.rhs + "\n"); // li r, i
    }

    public void visit(Move_Reg_FuncName n) {
        String lhsName = n.lhs.toString();
        String funcName = n.rhs.toString();
        instructions.add("    la " + lhsName + ", " + mangle(funcName) + "\n"); // la r, Fun
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
        instructions.add("    mv a0, " + contentReg + "\n");
        instructions.add("    jal print\n");
    }

    public void visit(ErrorMessage n) {
        String msg = n.msg;
        if (msg.contains("null")) {
            instructions.add("    la a0, msg_nullptr\n");
        } else if (msg.contains("out")) {
            instructions.add("    la a0, msg_array_oob\n");
        }
        instructions.add("    j error\n");
    }

    public void visit(Goto n) {
        String label = n.label.toString();
        instructions.add("    j " + currentFunctionName + "_" + label + "\n");
    }

    public void visit(IfGoto n) {
        String cond = n.condition.toString();
        String label = n.label.toString();
        String skip = currentFunctionName + "_ifgoto_skip_" + (labelCounter++);
        instructions.add("    bne " + cond + ", x0, " + skip + "\n");
        instructions.add("    j " + currentFunctionName + "_" + label + "\n");
        instructions.add(skip + ":\n");
    }

    public void visit(Call n) {
        String lhs = n.lhs.toString();
        String callee = n.callee.toString();
        List<Identifier> args = n.args;

        // store args
        // instructions.add("////// SAVING ARGS\n");
        saveArgs(args);
        // make the call
        // instructions.add("////// CALL\n");
        instructions.add("    jalr ra, " + callee + ", 0\n");
        // move return val into lhs
        // instructions.add("////// SAVE RETURN VAL\n");
        instructions.add("    mv " + lhs + ", a0\n");
        // clean up stack
        // instructions.add("////// CLEAN UP STACK\n");
        cleanArgs(args);
        // instructions.add("////// EXIT CALL INSTR\n");
    }

    /*
     * helpers
     */

    String getEntryFunction(List<FunctionDecl> funcs) {
        return funcs.get(0).functionName.toString();
    }

    void addPrelude(String entryFunctionName) {
        program.add(".equiv @sbrk, 9\n");
        program.add(".equiv @print_string, 4\n");
        program.add(".equiv @print_char, 11\n");
        program.add(".equiv @print_int, 1\n");
        program.add(".equiv @exit, 10\n");
        program.add(".equiv @exit2, 17\n\n");

        program.add(".text\n");
        program.add(".globl main\n");
        program.add("main:\n");
        program.add(" jal " + mangle(entryFunctionName) + "\n");
        program.add(" li a0, @exit\n");
        program.add(" ecall\n\n");
    }

    void addPostlude() {

        program.add("\n.globl print\n");
        program.add("print:\n");
        program.add("    mv a1, a0\n");
        program.add("    li a0, @print_int\n");
        program.add("    ecall\n");
        program.add("    li a1, 10\n");
        program.add("    li a0, @print_char\n");
        program.add("    ecall\n");
        program.add("    jr ra\n\n");

        program.add(".globl error\n");
        program.add("error:\n");
        program.add("    mv a1, a0\n");
        program.add("    li a0, @print_string\n");
        program.add("    ecall\n");
        program.add("    li a1, 10\n");
        program.add("    li a0, @print_char\n");
        program.add("    ecall\n");
        program.add("    li a0, @exit\n");
        program.add("    ecall\n");
        program.add("abort_17:\n");
        program.add("    j abort_17\n\n");

        program.add(".globl alloc\n");
        program.add("alloc:\n");
        program.add("    mv a1, a0\n");
        program.add("    li a0, @sbrk\n");
        program.add("    ecall\n");
        program.add("    jr ra\n\n");
        program.add(".data\n\n");

        program.add(".globl msg_nullptr\n");
        program.add("msg_nullptr:\n");
        program.add("    .asciiz \"null pointer\"\n");
        program.add("    .align 2\n\n");

        program.add(".globl msg_array_oob\n");
        program.add("msg_array_oob:\n");
        program.add("    .asciiz \"array index out of bounds\"\n");
        program.add("    .align 2\n");
    }

    void saveArgs(List<Identifier> args) {
        // allocate space
        int nArgs = args.size();
        instructions.add("    li t6, " + nArgs * 4 + "\n");
        instructions.add("    sub sp, sp, t6\n");

        // store args
        int offset = 0;
        for (Identifier a : args) {
            String argName = a.toString();
            Integer frameOffset = offsets.get(argName);
            if (frameOffset == null) {
                // identifier never written to; callee reads from a2/a3/... anyway
                instructions.add("    sw x0, " + offset + "(sp)\n");
            } else {
                instructions.add("    lw t6, " + frameOffset + "(fp)\n");
                instructions.add("    sw t6, " + offset + "(sp)\n");
            }
            offset += 4;
        }
    }

    void cleanArgs(List<Identifier> args) {
        int nArgs = args.size();
        instructions.add("    li t6, " + nArgs * 4 + "\n");
        instructions.add("    add sp, sp, t6\n");
    }

    // return local stack-allocated variables (unique, excluding params)
    List<String> getLocals(Block b, List<Identifier> params) {
        Set<String> paramNames = new HashSet<>();
        for (Identifier p : params)
            paramNames.add(p.toString());
        LinkedHashSet<String> vars = new LinkedHashSet<>();
        for (Instruction i : b.instructions) {
            if (i instanceof Move_Id_Reg) {
                String lhs = ((Move_Id_Reg) i).lhs.toString();
                if (!paramNames.contains(lhs))
                    vars.add(lhs);
            } else if (i instanceof Move_Reg_Id) {
                String rhs = ((Move_Reg_Id) i).rhs.toString();
                if (!paramNames.contains(rhs))
                    vars.add(rhs);
            }
        }
        return new ArrayList<>(vars);
    }

    // offset(var) = x if the value of var is at x(fp)
    Map<String, Integer> getOffsets(List<Identifier> params, List<String> locals) {
        Map<String, Integer> offsets = new HashMap<>();
        int offset = -12;
        for (String local : locals) {
            offsets.put(local, offset);
            offset -= 4;
        }
        offset = 0;
        for (Identifier p : params) {
            offsets.put(p.toString(), offset);
            offset += 4;
        }
        return offsets;
    }

    public List<String> getProgram() {
        return program;
    }

}
