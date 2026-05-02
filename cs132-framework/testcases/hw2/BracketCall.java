// VALID: method call on a parenthesized expression — (expr).method().
// Since MessageSend requires a PrimaryExpression receiver, and BracketExpression
// is a PrimaryExpression, (expr).method() chains two calls.
class Main {
    public static void main(String[] a) {
        System.out.println(new Outer().run());
    }
}

class Inner {
    public int val() { return 42; }
}

class Outer {
    public Inner make() {
        return new Inner();
    }
    public int run() {
        return (this.make()).val();
    }
}
