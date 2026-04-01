import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class LexerException extends RuntimeException {
    public LexerException(int pos) {
        super(String.format("could not consume token starting at position %d", pos));
    }
}

public class Lexer {
    private String source;
    private int sourceLength;
    private int charPtr = 0;
    private List<TokenType> tokenList = new ArrayList<TokenType>();
    private List<Scanner> scanners =
            new ArrayList<Scanner>(
                    Arrays.asList(
                            new Scanner(TokenType.LBRACE),
                            new Scanner(TokenType.RBRACE),
                            new Scanner(TokenType.LPAR),
                            new Scanner(TokenType.RPAR),
                            new Scanner(TokenType.PRINT),
                            new Scanner(TokenType.SEMI),
                            new Scanner(TokenType.IF),
                            new Scanner(TokenType.ELSE),
                            new Scanner(TokenType.WHILE),
                            new Scanner(TokenType.TRUE),
                            new Scanner(TokenType.FALSE),
                            new Scanner(TokenType.NEG)));

    // lexer constructor
    public Lexer(String source) {
        this.source = source;
        this.sourceLength = this.source.length();
    }

    private String tokenTypeToStringLiteral(TokenType t) {
        switch (t) {
            case LBRACE:
                return "{";
            case RBRACE:
                return "}";
            case LPAR:
                return "(";
            case RPAR:
                return ")";
            case PRINT:
                return "System.out.println";
            case SEMI:
                return ";";
            case IF:
                return "if";
            case ELSE:
                return "else";
            case WHILE:
                return "while";
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case NEG:
                return "!";
            case EOF:
                return "\0";
            default:
                return "error";
        }
    }

    private boolean isAtEnd() {
        return charPtr >= sourceLength;
    }

    private void skipWhitespace() {
        if (isAtEnd()) return;
        while (charPtr < sourceLength
                && (source.charAt(charPtr) == ' '
                        || source.charAt(charPtr) == '\n'
                        || source.charAt(charPtr) == '\t')) {
            ++charPtr;
        }
    }

    private void consumeToken() throws LexerException {
        skipWhitespace();
        if (isAtEnd()) {
            return;
        }
        // take first match
        for (Scanner s : scanners) {
            if (s.scan()) return;
        }
        throw new LexerException(charPtr);
    }

    // consume tokens.
    // throws exception if can't consume token
    public void tokenize() throws LexerException {
        while (!isAtEnd()) {
            consumeToken();
        }
        tokenList.add(TokenType.EOF);
    }

    public List<TokenType> getTokenList() {
        return tokenList;
    }

    private class Scanner {
        private String target;
        private int maxLen;
        private TokenType token;

        // define a scanner for a given token
        // scanner is naive and only works for string literals
        public Scanner(TokenType tokenType) {
            this.token = tokenType;
            this.target = tokenTypeToStringLiteral(token);
            this.maxLen = this.target.length();
        }

        // consumes source one char at a time, matching against target
        // updates charPtr if scan is successful
        // adds token to token list if successful
        public boolean scan() {
            int pos = charPtr; // temp poiner
            for (int i = 0; i < maxLen; ++i) {
                // check bounds + character match
                if (pos >= sourceLength || target.charAt(i) != source.charAt(pos)) {
                    return false;
                }
                ++pos;
            }

            // successful scan:
            // update charPtr
            // update current token
            // add token to token list
            charPtr = pos;
            tokenList.add(token);
            return true;
        }
    }
}
