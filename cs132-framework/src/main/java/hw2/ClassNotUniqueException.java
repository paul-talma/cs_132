package hw2;

/** Thrown when two classes share the same name. */
public class ClassNotUniqueException extends TypeException {
    public ClassNotUniqueException() {
        super("Nonunique class name.");
    }

    public ClassNotUniqueException(String className) {
        super(String.format("Nonunique class name: %s", className));
    }
}
