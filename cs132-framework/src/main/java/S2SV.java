import IR.ParseException;
import IR.SparrowParser;
import hw4.Translator;

public class S2SV {
    public static void main(String[] args) throws ParseException, java.io.IOException {
        // --no-X flags can only disable features that are hardcoded true in
        // Config.java.
        // They cannot re-enable a feature that has been hardcoded to false.
        for (String arg : args) {
            if (arg.equals("--no-chordal"))
                hw4.Config.CHORDAL = false;
            if (arg.equals("--no-arg-regs"))
                hw4.Config.ARG_REGS = false;
            if (arg.equals("--no-coalescing"))
                hw4.Config.COALESCING = false;
            if (arg.equals("--no-spill-cost"))
                hw4.Config.SPILL_COST = false;
            if (arg.equals("--no-devirt"))
                hw4.Config.DEVIRT = false;
            if (arg.equals("--no-dce"))
                hw4.Config.DCE = false;
            if (arg.equals("--no-remat"))
                hw4.Config.REMAT = false;
        }

        hw4.FunctionAllocator allocator = hw4.Config.CHORDAL
                ? new hw4.ChordalAllocator()
                : new hw4.LinearScanWrapper();

        SparrowParser parser = new SparrowParser(System.in);
        IR.syntaxtree.Program program = parser.Program();
        Translator t = new Translator(allocator);
        t.visit(program);
        sparrowv.Program sparrowvProgram = t.getProgram();
        System.out.print(sparrowvProgram.toString());
    }
}
