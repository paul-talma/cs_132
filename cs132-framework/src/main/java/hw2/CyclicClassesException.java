package hw2;

/** Thrown when a class's inheritance chain contains a cycle. */
public class CyclicClassesException extends TypeException {
    public CyclicClassesException(String className) {
        super(String.format("Class %s is part of a type circularity!", className));
    }
}
