import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Parse {
    public static void main(String[] args) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(args[0])));
        Lexer lexer = new Lexer(source);
        try {
            lexer.tokenize();
        } catch (LexerException e) {
            System.out.println("Parse error");
            return;
        }
        Parser parser = new Parser(lexer.getTokenList());
    }
}
