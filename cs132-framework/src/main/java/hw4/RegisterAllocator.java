package hw4;

import IR.syntaxtree.Add;
import IR.syntaxtree.Alloc;
import IR.syntaxtree.Call;
import IR.syntaxtree.ErrorMessage;
import IR.syntaxtree.FunctionDeclaration;
import IR.syntaxtree.Goto;
import IR.syntaxtree.If;
import IR.syntaxtree.IfGoto;
import IR.syntaxtree.Instruction;
import IR.syntaxtree.LabelWithColon;
import IR.syntaxtree.LessThan;
import IR.syntaxtree.Load;
import IR.syntaxtree.Move;
import IR.syntaxtree.Multiply;
import IR.syntaxtree.Print;
import IR.syntaxtree.Program;
import IR.syntaxtree.SetFuncName;
import IR.syntaxtree.SetInteger;
import IR.syntaxtree.Store;
import IR.syntaxtree.Subtract;
import IR.token.FunctionName;
import IR.token.Identifier;
import IR.token.Label;
import IR.token.Register;
import sparrowv.LabelInstr;
import sparrowv.Move_Id_Reg;
import sparrowv.Move_Reg_FuncName;
import sparrowv.Move_Reg_Id;
import sparrowv.Move_Reg_Integer;
import sparrowv.Move_Reg_Reg;
import java.util.*;

/**
 * Trivial register allocator: all Sparrow variables stay in the Sparrow-V
 * identifier (variable-environment) namespace.  Three scratch registers are
 * used for in-flight computation and are never exposed as call args or return
 * values.  This satisfies the Sparrow-V invariant: identifiers only appear in
 * Move_Reg_Id / Move_Id_Reg and in call-arg / formal-param / return positions.
 */
public class RegisterAllocator {

    static final String RA = "t3";   // scratch for first source / result
    static final String RB = "t4";   // scratch for second source / callee ptr
    static final String RC = "t5";   // scratch for load/alloc destination

    IR.syntaxtree.Program program;

    public RegisterAllocator(IR.syntaxtree.Program program) {
        this.program = program;
    }

    public void allocate() {
        List<sparrowv.FunctionDecl> funDecls = new ArrayList<>();
        for (Enumeration<IR.syntaxtree.Node> e = program.f0.elements(); e.hasMoreElements();) {
            FunctionDeclaration fd = (FunctionDeclaration) e.nextElement();
            funDecls.add(translateFunction(fd));
        }
        System.out.print(new sparrowv.Program(funDecls).toString());
    }

    sparrowv.FunctionDecl translateFunction(FunctionDeclaration fd) {
        // Formal parameters: keep their original identifier names
        List<Identifier> formalParams = new ArrayList<>();
        if (fd.f3.present()) {
            for (Enumeration<IR.syntaxtree.Node> e = fd.f3.elements(); e.hasMoreElements();) {
                IR.syntaxtree.Identifier id = (IR.syntaxtree.Identifier) e.nextElement();
                formalParams.add(new Identifier(id.f0.toString()));
            }
        }

        // Emit block
        sparrowv.Block block = emitBlock(fd.f5);

        return new sparrowv.FunctionDecl(
            new FunctionName(fd.f1.f0.toString()),
            formalParams,
            block
        );
    }

    sparrowv.Block emitBlock(IR.syntaxtree.Block block) {
        List<sparrowv.Instruction> instrs = new ArrayList<>();

        if (block.f0.present()) {
            for (Enumeration<IR.syntaxtree.Node> e = block.f0.elements(); e.hasMoreElements();) {
                emitInstruction((Instruction) e.nextElement(), instrs);
            }
        }

        // Return: the return variable lives in the identifier namespace
        String retId = block.f2.f0.toString();
        return new sparrowv.Block(instrs, new Identifier(retId));
    }

    void emitInstruction(Instruction instr, List<sparrowv.Instruction> out) {
        IR.syntaxtree.Node n = instr.f0.choice;

        if (n instanceof LabelWithColon) {
            LabelWithColon lwc = (LabelWithColon) n;
            out.add(new LabelInstr(new Label(lwc.f0.f0.toString())));

        } else if (n instanceof SetInteger) {
            SetInteger si = (SetInteger) n;
            String dest = si.f0.f0.toString();
            // RA = N; dest = RA
            out.add(new Move_Reg_Integer(new Register(RA), Integer.parseInt(si.f2.f0.toString())));
            out.add(new Move_Id_Reg(new Identifier(dest), new Register(RA)));

        } else if (n instanceof SetFuncName) {
            SetFuncName sfn = (SetFuncName) n;
            String dest = sfn.f0.f0.toString();
            // RA = @func; dest = RA
            out.add(new Move_Reg_FuncName(new Register(RA), new FunctionName(sfn.f3.f0.toString())));
            out.add(new Move_Id_Reg(new Identifier(dest), new Register(RA)));

        } else if (n instanceof Add) {
            Add a = (Add) n;
            emitBinOp(a.f0.f0.toString(), a.f2.f0.toString(), a.f4.f0.toString(), "add", out);

        } else if (n instanceof Subtract) {
            Subtract s = (Subtract) n;
            emitBinOp(s.f0.f0.toString(), s.f2.f0.toString(), s.f4.f0.toString(), "sub", out);

        } else if (n instanceof Multiply) {
            Multiply m = (Multiply) n;
            emitBinOp(m.f0.f0.toString(), m.f2.f0.toString(), m.f4.f0.toString(), "mul", out);

        } else if (n instanceof LessThan) {
            LessThan lt = (LessThan) n;
            emitBinOp(lt.f0.f0.toString(), lt.f2.f0.toString(), lt.f4.f0.toString(), "lt", out);

        } else if (n instanceof Load) {
            Load l = (Load) n;
            String dest = l.f0.f0.toString();
            String addr = l.f3.f0.toString();
            int offset = Integer.parseInt(l.f5.f0.toString());
            // RA = addr; RC = [RA + offset]; dest = RC
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(addr)));
            out.add(new sparrowv.Load(new Register(RC), new Register(RA), offset));
            out.add(new Move_Id_Reg(new Identifier(dest), new Register(RC)));

        } else if (n instanceof Store) {
            Store s = (Store) n;
            String addr = s.f1.f0.toString();
            String src = s.f6.f0.toString();
            int offset = Integer.parseInt(s.f3.f0.toString());
            // RA = addr; RB = src; [RA + offset] = RB
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(addr)));
            out.add(new Move_Reg_Id(new Register(RB), new Identifier(src)));
            out.add(new sparrowv.Store(new Register(RA), offset, new Register(RB)));

        } else if (n instanceof Move) {
            Move m = (Move) n;
            String dest = m.f0.f0.toString();
            String src = m.f2.f0.toString();
            // RA = src; dest = RA
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(src)));
            out.add(new Move_Id_Reg(new Identifier(dest), new Register(RA)));

        } else if (n instanceof Alloc) {
            Alloc a = (Alloc) n;
            String dest = a.f0.f0.toString();
            String size = a.f4.f0.toString();
            // RA = size; RC = alloc(RA); dest = RC
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(size)));
            out.add(new sparrowv.Alloc(new Register(RC), new Register(RA)));
            out.add(new Move_Id_Reg(new Identifier(dest), new Register(RC)));

        } else if (n instanceof Print) {
            Print p = (Print) n;
            String src = p.f2.f0.toString();
            // RA = src; print(RA)
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(src)));
            out.add(new sparrowv.Print(new Register(RA)));

        } else if (n instanceof ErrorMessage) {
            ErrorMessage em = (ErrorMessage) n;
            out.add(new sparrowv.ErrorMessage(em.f2.f0.toString()));

        } else if (n instanceof Goto) {
            Goto g = (Goto) n;
            out.add(new sparrowv.Goto(new Label(g.f1.f0.toString())));

        } else if (n instanceof IfGoto) {
            IfGoto ig = (IfGoto) n;
            String cond = ig.f1.f0.toString();
            // RA = cond; if0 RA goto L
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(cond)));
            out.add(new sparrowv.IfGoto(new Register(RA), new Label(ig.f3.f0.toString())));

        } else if (n instanceof If) {
            If i = (If) n;
            String cond = i.f1.f0.toString();
            out.add(new Move_Reg_Id(new Register(RA), new Identifier(cond)));
            out.add(new sparrowv.IfGoto(new Register(RA), new Label(i.f3.f0.toString())));

        } else if (n instanceof Call) {
            emitCall((Call) n, out);
        }
    }

    void emitBinOp(String dest, String src1, String src2, String op,
                   List<sparrowv.Instruction> out) {
        // RA = src1; RB = src2; RA = RA op RB; dest = RA
        out.add(new Move_Reg_Id(new Register(RA), new Identifier(src1)));
        out.add(new Move_Reg_Id(new Register(RB), new Identifier(src2)));
        switch (op) {
            case "add": out.add(new sparrowv.Add(new Register(RA), new Register(RA), new Register(RB))); break;
            case "sub": out.add(new sparrowv.Subtract(new Register(RA), new Register(RA), new Register(RB))); break;
            case "mul": out.add(new sparrowv.Multiply(new Register(RA), new Register(RA), new Register(RB))); break;
            case "lt":  out.add(new sparrowv.LessThan(new Register(RA), new Register(RA), new Register(RB))); break;
        }
        out.add(new Move_Id_Reg(new Identifier(dest), new Register(RA)));
    }

    void emitCall(Call c, List<sparrowv.Instruction> out) {
        String dest = c.f0.f0.toString();
        String callee = c.f3.f0.toString();

        // Load function pointer into RB
        out.add(new Move_Reg_Id(new Register(RB), new Identifier(callee)));

        // Build arg list: keep as identifier names (looked up in caller's variable env)
        List<Identifier> argIds = new ArrayList<>();
        if (c.f5.present()) {
            for (Enumeration<IR.syntaxtree.Node> e = c.f5.elements(); e.hasMoreElements();) {
                IR.syntaxtree.Identifier id = (IR.syntaxtree.Identifier) e.nextElement();
                argIds.add(new Identifier(id.f0.toString()));
            }
        }

        // RA = call RB(args...); dest = RA
        out.add(new sparrowv.Call(new Register(RA), new Register(RB), argIds));
        out.add(new Move_Id_Reg(new Identifier(dest), new Register(RA)));
    }
}
