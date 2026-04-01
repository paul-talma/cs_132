import java.util.List;

// grammar:
// start: S
// S ::= { L }
//     | System.out.println ( E ) ;
//     | if ( E ) S else S
//     | while ( E ) S
// L ::= S L
//     | ϵ
// E ::= true
//     | false
//     | ! E

class ParserError extends RuntimeException {
    ParserError(TokenType token, TokenType currToken) {
        super(String.format("expecting token of type %s but got %s", token, currToken));
    }
}

public class Parser {
    private List<TokenType> tokenList;
    private int tokenPtr = 0;
    private TokenType currToken;

    public Parser(List<TokenType> tokenList) {
        this.tokenList = tokenList;
        if (tokenList.size() > 0) {
            currToken = tokenList.get(0);
        }
    }

    private void eat(TokenType token) throws ParserError {
        if (currToken == token) {
            ++tokenPtr;
            currToken = tokenList.get(tokenPtr);
        } else {
            throw new ParserError(token, currToken);
        }
    }

    private boolean parseS() {
        boolean success = true;
        switch (currToken) {
            case LBRACE:
                // { L }
                eat(TokenType.LBRACE);
                success = success && parseL();
                eat(TokenType.RBRACE);
                break;
            case PRINT:
                // System.out.println ( E ) ;
                eat(TokenType.PRINT);
                eat(TokenType.LPAR);
                success = success && parseE();
                eat(TokenType.RPAR);
                eat(TokenType.SEMI);
                break;
            case IF:
                // if ( E ) S else S
                eat(TokenType.IF);
                eat(TokenType.LPAR);
                success = success && parseE();
                eat(TokenType.RPAR);
                success = success && parseS();
                eat(TokenType.ELSE);
                success = success && parseS();
                break;

            case WHILE:
                // while ( E ) S
                eat(TokenType.WHILE);
                eat(TokenType.LPAR);
                success = success && parseE();
                eat(TokenType.RPAR);
                success = success && parseS();
                break;
            default:
                success = false;
        }
        return success;
    }

    private boolean parseL() {
        // L ::= S L
        //     | ϵ
        if (currToken == TokenType.RBRACE) return true;
        return parseS() && parseL();
    }

    private boolean parseE() {
        boolean success = true;
        switch (currToken) {
            case TRUE:
                // true
                eat(TokenType.TRUE);
                break;
            case FALSE:
                // false
                eat(TokenType.FALSE);
                break;
            case NEG:
                // ! E
                eat(TokenType.NEG);
                success = parseE();
                break;
            default:
                success = false;
                break;
        }
        return success;
    }

    public boolean parse() {
        boolean success;
        success = parseS();
        return success && currToken == TokenType.EOF;
    }
}
