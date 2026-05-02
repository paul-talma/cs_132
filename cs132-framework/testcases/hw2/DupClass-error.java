// ERROR: two classes with the same name.
// Rule 21: classnames must be distinct.
class Main {
    public static void main(String[] a) {
        System.out.println(0);
    }
}

class Foo {
    public int x() { return 1; }
}

class Foo {
    public int y() { return 2; }
}
