// ERROR: two fields with the same name declared in the same class.
// Rule 23/24: distinct(id_1,...,id_f) must hold within the class.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    int x;
    int x;
    public int run() { return x; }
}
