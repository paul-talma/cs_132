// VALID: child overrides parent methods with identical signatures.
// noOverloading requires signatures to match, and they do.
// Note: MiniJava binary ops require PrimaryExpression on both sides, so
// multi-operand chains like a+b+c need brackets: (a+b)+c.
class Main {
    public static void main(String[] a) {
        System.out.println(new Child().run());
    }
}

class Parent {
    public int run() { return 1; }
    public boolean check(int x) { return true; }
    public int add(int a, int b) { return a + b; }
}

class Child extends Parent {
    public int run() { return 2; }
    public boolean check(int y) { return false; }
    public int add(int x, int y) { return x + y; }
}
