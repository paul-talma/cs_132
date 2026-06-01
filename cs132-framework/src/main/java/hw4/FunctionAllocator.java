package hw4;

import java.util.Map;
import java.util.Set;

import IR.syntaxtree.FunctionDeclaration;

// Common interface for per-function register allocators.
// ChordalAllocator and LinearScanWrapper both implement this so the
// Translator can work with either without knowing the implementation.
public interface FunctionAllocator {
    FunctionAllocation allocate(FunctionDeclaration n);
    boolean isLiveAtEntry(String varName);
    Set<String> getLiveOutAt(int instrIdx);
    Map<String, String> getDevirtMap();
    boolean isReachable(int instrIdx);
    Object getRematerialValue(int instrIdx, String var);
}
