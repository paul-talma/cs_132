// ERROR: if condition must be boolean (rule 29), not int.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public int run() {
        int x;
        x = 5;
        if (x)
            x = 1;
        else
            x = 2;
        return x;
    }
}
