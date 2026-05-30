import minijava.syntaxtree.*;
import minijava.MiniJavaParser;
import minijava.ParseException;
import hw3.J2SCompiler;

public class J2S {
    public static void main(String[] args) throws ParseException, java.io.IOException {
        MiniJavaParser parser = new MiniJavaParser(System.in);
        Goal root = parser.Goal();
        J2SCompiler compiler = new J2SCompiler(root);
        compiler.compile();
    }
}
