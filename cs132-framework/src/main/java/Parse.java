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

        // System.out.println("Received input:");
        // System.out.println(source);

        Lexer lexer = new Lexer(source);
        try {
            lexer.tokenize();
        } catch (LexerException e) {
            System.out.println(e);
            return;
        }
        System.out.println("Successfully tokenized input!");
        System.out.println(lexer.getTokenList());
        // Parser parser = new Parser(lexer.getTokenList());
    }
}
