// ERROR: && requires boolean operands (rule 32), not int.
// Note: MiniJava Statement does not include `return`, so we use the method
// return expression to trigger the check.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public boolean run() {
        int x;
        int y;
        x = 1;
        y = 2;
        return x && y;
    }
}
