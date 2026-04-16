import java.io.IOException;
import java.util.Scanner;
import syntaxtree.*;
import visitor.*;

public class Typecheck {
    private static String getSource() {
        String source = "";
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            source = sb.toString();
            if (source.endsWith("\n"))
                source = source.substring(0, source.length() - 1);
        }
        return source;
    }

    public static void main(String[] args) throws IOException {
        String source = getSource();
        // TODO: parse with MiniJavaParser, then run type checking visitor
    }
}
