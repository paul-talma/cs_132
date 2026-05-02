// VALID: deeply nested bracket expressions (rule 48: (e):t preserves type).
// Also exercises bracket as PrimaryExpression enabling chained operations.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public int run() {
        int x;
        boolean b;
        x = 5;
        b = true;
        if ((((b)))) {
            x = (((x + (1 + 1))));
        } else {
            x = 0;
        }
        return (((x)));
    }
}
