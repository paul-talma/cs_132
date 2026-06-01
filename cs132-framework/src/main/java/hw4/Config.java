package hw4;

// Feature flags — edit these directly to control the allocator configuration.
// Command-line --no-X flags can additionally disable a feature at runtime but
// cannot re-enable one that has been hardcoded to false here.
public class Config {
    public static boolean CHORDAL = true; // chordal MCS+greedy vs linear scan
    public static boolean ARG_REGS = true; // pass parameters in a2–a7
    public static boolean COALESCING = true; // register coalescing
    public static boolean SPILL_COST = true; // loop-depth spill cost heuristic
    public static boolean DEVIRT = true; // devirtualization of static calls
    public static boolean DCE = true; // dead code elimination (unreachable block removal)
    public static boolean REMAT = true; // rematerialize cheap spilled constants/funcnames
}
