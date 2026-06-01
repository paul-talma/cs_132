import IR.ParseException;
import IR.SparrowParser;
import hw4.Translator;

public class S2SV {
    public static void main(String[] args) throws ParseException, java.io.IOException {
        SparrowParser parser = new SparrowParser(System.in);
        IR.syntaxtree.Program program = parser.Program();
        Translator t = new Translator(new hw4.ChordalAllocator());
        t.visit(program);
        System.out.print(t.getProgram().toString());
    }
}
