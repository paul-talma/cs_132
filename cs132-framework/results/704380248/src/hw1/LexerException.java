package hw1;

public class LexerException extends RuntimeException {
    public LexerException(int pos) {
        super(String.format("could not consume token starting at position %d", pos));
    }
}
