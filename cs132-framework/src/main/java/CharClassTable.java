class CharClassTable {
    // maps single characters to CharClass objects
    public CharClass map(char c) {
        switch (c) {
            case '{':
                return CharClass.LBRACE;
            case '}':
                return CharClass.RBRACE;
            case '(':
                return CharClass.LPAR;
            case ')':
                return CharClass.RPAR;
            case '.':
                return CharClass.DOT;
            case '!':
                return CharClass.NEG;
            case ';':
                return CharClass.SEMI;
        }
        if (Character.isLetter(c)) {
            return CharClass.ALPHA;
        }
        return CharClass.UNK;
    }
}
