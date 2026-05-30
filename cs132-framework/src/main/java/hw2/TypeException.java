package hw2;

/**
 * Base class for all MiniJava type errors. Thrown during type-checking and
 * caught at the top level in Typecheck.main. Extends RuntimeException so
 * callers are not required to declare it.
 */
public class TypeException extends RuntimeException {
    public TypeException(String msg) {
        super(msg);
    }
}
