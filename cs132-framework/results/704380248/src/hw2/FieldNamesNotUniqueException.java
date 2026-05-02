package hw2;

public class FieldNamesNotUniqueException extends TypeException {
    public FieldNamesNotUniqueException(String className) {
        super(String.format("Field names not unique in class %s", className));
    }
}
