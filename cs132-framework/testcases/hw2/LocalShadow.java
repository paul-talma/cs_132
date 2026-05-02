// VALID: local variable with same name as a class field.
// Rule 25: env = fields(C) . [params, locals] — locals take precedence.
// The distinct() check only covers params+locals, not fields. So this is legal.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    int x;
    public int run() {
        int x;
        x = 99;
        return x;
    }
}
