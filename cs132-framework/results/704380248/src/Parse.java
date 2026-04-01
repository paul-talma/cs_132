import java.io.IOException;
import java.util.Scanner;

public class Parse {
    private static String getSource() {
        String source = "";
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            source = sb.toString();
            if (source.endsWith("\n")) source = source.substring(0, source.length() - 1);
        }
        return source;
    }

    public static void main(String[] args) throws IOException {
        String source = getSource();
        boolean success = true;
        Lexer lexer = new Lexer(source);
        try {
            lexer.tokenize();
        } catch (LexerException e) {
            success = false;
        }
        if (success) {
            Parser parser = new Parser(lexer.getTokenList());
            try {
                success = parser.parse();
            } catch (ParserError e) {
                success = false;
            }
        }
        System.out.println(success ? "Program parsed successfully" : "Parse error");
    }
}
