import minijava.syntaxtree.*;
import minijava.MiniJavaParser;
import minijava.ParseException;

public class J2S {
    public static void main(String[] args) throws ParseException {
        MiniJavaParser parser = new MiniJavaParser(System.in);
        Goal root = parser.Goal();
        // TODO: compile root to Sparrow and print
    }
}
