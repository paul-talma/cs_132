package hw1;

public class Scanner {
    private String target;
    private int offset = 0;
    private int maxLen;

    public Scanner(String target) {
        this.target = target;
        this.maxLen = this.target.length();
    }

    public boolean scan(int charPtr) {
        while (offset < maxLen) {
            if (target.charAt(offset) != charPtr) {
                return false;
            }
            ++charPtr;
            ++offset;
        }
        return true;
    }
}
