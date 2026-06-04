import java.util.List;

import IR.ParseException;
import hw5.*;

public class SV2V {
    public static void main(String[] args) throws ParseException, java.io.IOException {
        IR.registers.Registers.SetRiscVregs();
        try {
            IR.syntaxtree.Node root = new IR.SparrowParser(System.in).Program();
            IR.visitor.SparrowVConstructor svc = new IR.visitor.SparrowVConstructor();
            root.accept(svc);
            sparrowv.Program program = svc.getProgram();
            SV2VCompiler compiler = new SV2VCompiler();
            program.accept(compiler);
            List<String> compiledProgram = compiler.getProgram();
            printProgram(compiledProgram);

        } catch (IR.ParseException e) {
            System.out.println(e.toString());
        }
    }

    static void printProgram(List<String> program) {
        String progString = "".join("", program);
        System.out.print(progString);
    }
}
