package hw2;

public class CyclicClassesException extends TypeException {
    public CyclicClassesException(String className) {
        super(String.format("Class %s is part of a type circularity!", className));
    }
}
