// ERROR: a local variable has the same name as a formal parameter.
// Rule 25: distinct(id_1^F,...,id_n^F, id_1,...,id_r) — params and locals must
// be pairwise distinct.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run(1));
    }
}

class Obj {
    public int run(int x) {
        int x;
        x = 2;
        return x;
    }
}
