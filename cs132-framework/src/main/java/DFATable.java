class DFATable {
    // get() maps (state, charClass) to next state
    private int[][] transition;

    public Integer get(Integer state, CharClass charClass) {
        return transition[state][charClass.ordinal()];
    }

    // pass an array of [state][charclass] transitions
    public DFATable(int nStates, int nClasses, int[][] transitions) {
        transition = transitions;
    }
}
