package hw2;

/** Thrown when a class declares two fields with the same name. */
public class FieldNamesNotUniqueException extends TypeException {
    public FieldNamesNotUniqueException(String className) {
        super(String.format("Field names not unique in class %s", className));
    }
}
