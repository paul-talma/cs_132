import java.io.IOException;
import minijava.syntaxtree.*;
import minijava.MiniJavaParser;
import minijava.ParseException;
import hw2.*;

public class Typecheck {
    public static void main(String[] args) throws ParseException, IOException {
        java.io.InputStream input = System.in;
        MiniJavaParser parser = new MiniJavaParser(input);
        Goal root = parser.Goal();
        TypeChecker tc = new TypeChecker(root);
        try {
            tc.typeCheck();
            System.out.println("Program type checked successfully");
        } catch (Exception e) {
            System.out.println("Type error");
        }
    }
}
