package hw1;

public class ParserError extends RuntimeException {
    ParserError(TokenType token, TokenType currToken) {
        super(String.format("expecting token of type %s but got %s", token, currToken));
    }
}
