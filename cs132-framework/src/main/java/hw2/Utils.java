package hw2;

import syntaxtree.Identifier;

public class Utils {
    private Utils() {
    }

    public static String getIDNameFromIDNode(Identifier n) {
        return n.f0.toString();
    }
}
