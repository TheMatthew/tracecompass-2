package org.eclipse.tracecompass.internal.analysis.timing.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents a node in the Call Graph
 *
 * @author Sonia Farrah
 *
 */
public class CallGraphNode {

    private final Object fSymbol;
    private final Collection<CallGraphNode> fCallers = new ArrayList<>();
    private final Map<CallGraphNode, Integer> fChildrenCount = new HashMap<>();
    private final Map<Object, CallGraphNode> fChildrenBySymbol = new HashMap<>();
    private final int fPid;
    private long fDuration = 0;

    /**
     * Constructor
     *
     * @param symbol
     *            The node symbol
     * @param duration
     *            duration of the node
     * @param pid
     */
    public CallGraphNode(Object symbol, long duration, int pid) {
        fSymbol = symbol;
        fDuration = duration;
        fPid = pid;
    }

    /**
     * The node symbol
     *
     * @return The node symbol
     */
    public Object getSymbol() {
        return fSymbol;
    }

    /**
     * The callers
     *
     * @return The callers
     */
    public Collection<CallGraphNode> getCallers() {
        return fCallers;
    }

    /**
     * The callers
     *
     * @param callers
     *            The callers
     */
    public void setCallers(Collection<CallGraphNode> callers) {
        fCallers.clear();
        fCallers.addAll(callers);
    }

    /**
     * Callees
     *
     * @return The callees
     */
    public @NonNull Map<CallGraphNode, Integer> getChildren() {
        return fChildrenCount;
    }

    /**
     * The duration
     *
     * @return The duration
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * Add a caller
     *
     * @param parent
     *            The caller
     */
    public void addParent(CallGraphNode parent) {
        if (!fCallers.contains(parent)) {
            fCallers.add(parent);
        }
    }

    /**
     * Add a child
     *
     * @param child
     *            The calle to add
     */
    public void addChild(CallGraphNode child) {
        Object symbol = child.getSymbol();
        CallGraphNode node = fChildrenBySymbol.get(symbol);
        if (node == null) {
            node = new CallGraphNode(child.fSymbol, 0, fPid);
            fChildrenBySymbol.put(symbol, node);
        }
        node.addDuration(child.getDuration());
        Integer count = fChildrenCount.get(node);
        if (count == null) {
            count = 0;
        }
        count++;
        fChildrenCount.put(node, count);
    }

    private void addDuration(long duration) {
        fDuration += duration;
    }

    @Override
    public int hashCode() {
        return fSymbol.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CallGraphNode other = (CallGraphNode) obj;
        if (!fSymbol.equals(other.fSymbol)) {
            return false;
        }
        return true;
    }

    public int getPid() {
        return fPid;
    }

}
