// ERROR: two methods with the same name in the same class.
// Rule 23: distinct(methodname(m_1),...,methodname(m_k)) must hold.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public int run() { return 1; }
    public int run() { return 2; }
}
