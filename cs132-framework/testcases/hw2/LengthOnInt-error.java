// ERROR: .length requires int[] (rule 38), not int.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public int run() {
        int x;
        x = 5;
        return x.length;
    }
}
