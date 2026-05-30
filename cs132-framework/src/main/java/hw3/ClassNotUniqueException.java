package hw3;

/** Thrown when two classes share the same name. */
public class ClassNotUniqueException extends TypeException {
    public ClassNotUniqueException(String className) {
        super(String.format("Nonunique class name: %s", className));
    }
}
