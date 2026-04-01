import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class LexerException extends RuntimeException {
    public LexerException(int pos) {
        super(String.format("Lexer error: could not consume token starting at position %d", pos));
    }
}

public class Lexer {
    private String source;
    private int sourceLength;
    private int charPtr = 0;
    private char currChar;
    private List<TokenType> tokenList = new ArrayList<TokenType>();
    // private TokenType currentToken;
    // private Scanner scannerLBRACE = new Scanner(TokenType.LBRACE);
    // private Scanner scannerRBRACE = new Scanner(TokenType.RBRACE);
    // private Scanner scannerLPAR = new Scanner(TokenType.LPAR);
    // private Scanner scannerRPAR = new Scanner(TokenType.RPAR);
    // private Scanner scannerPRINT = new Scanner(TokenType.PRINT);
    // private Scanner scannerSEMI = new Scanner(TokenType.SEMI);
    // private Scanner scannerIF = new Scanner(TokenType.IF);
    // private Scanner scannerELSE = new Scanner(TokenType.ELSE);
    // private Scanner scannerWHILE = new Scanner(TokenType.WHILE);
    // private Scanner scannerTRUE = new Scanner(TokenType.TRUE);
    // private Scanner scannerFALSE = new Scanner(TokenType.FALSE);
    // private Scanner scannerNEG = new Scanner(TokenType.NEG);
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
        this.currChar = source.charAt(charPtr);
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
        }
        return "";
    }

    private boolean isAtEnd() {
        return charPtr >= sourceLength;
    }

    private void skipWhitespace() {
        while (currChar == ' ' || currChar == '\n' || currChar == '\t') {
            ++charPtr;
            if (isAtEnd()) {
                return;
            }
            currChar = source.charAt(charPtr);
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
                if (pos >= sourceLength || target.charAt(pos) != source.charAt(pos)) {
                    return false;
                }
                ++pos;
            }

            // successful scan:
            // update charPtr
            // update current token
            // add token to token list
            charPtr = pos;
            // currentToken = token;
            tokenList.add(token);
            return true;
        }
    }
}
