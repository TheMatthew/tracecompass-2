/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Yuriy Vashchuk (yvashchuk@gmail.com) - Initial API and implementation
 *   Patrick Tasse - Refactoring
 *   Vincent Perot - Add subfield filtering
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * The base class for the Filter tree nodes
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @author Patrick Tasse
 * @since 1.1
 */
public abstract class TmfFilterTreeNode implements ITmfFilterTreeNode, Cloneable {

    private static final String[] VALID_CHILDREN = {
            SegmentFilterMatchesNode.NODE_NAME
    };

    @Nullable private ITmfFilterTreeNode parent = null;
    private ArrayList<ITmfFilterTreeNode> children = new ArrayList<>();

    /**
     * @param parent
     *            the parent node
     */
    public TmfFilterTreeNode(@Nullable final ITmfFilterTreeNode parent) {
        if (parent != null) {
            parent.addChild(this);
        }
    }

    @Override
    public @Nullable ITmfFilterTreeNode getParent() {
        return parent;
    }

    @Override
    public abstract String getNodeName();

    @Override
    public boolean hasChildren() {
        return (children.size() > 0);
    }

    @Override
    public int getChildrenCount() {
        return children.size();
    }

    @Override
    public @NonNull ITmfFilterTreeNode[] getChildren() {
        return children.toArray(new org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode[0]);
    }

    @Override
    public ITmfFilterTreeNode getChild(final int index) throws IndexOutOfBoundsException {
        return children.get(index);
    }

    @Override
    public ITmfFilterTreeNode remove() {
        if (getParent() != null) {
            checkNotNull(getParent()).removeChild(this);
        }
        return this;
    }

    @Override
    public @Nullable ITmfFilterTreeNode removeChild(@Nullable ITmfFilterTreeNode node) {
        if (node != null) {
            children.remove(node);
            node.setParent(null);
            return node;
        }
        return null;
    }

    @Override
    public int addChild(@Nullable final ITmfFilterTreeNode node) {
        if (node != null) {
            node.setParent(this);
            if (children.add(node)) {
                return (children.size() - 1);
            }
        }
        return -1;
    }

    @Override
    public ITmfFilterTreeNode replaceChild(final int index, @Nullable final ITmfFilterTreeNode node) throws IndexOutOfBoundsException {
        @NonNull ITmfFilterTreeNode n = checkNotNull(node);
        n.setParent(this);
        return children.set(index, n);
    }

    @Override
    public void setParent(final @Nullable ITmfFilterTreeNode parent) {
        this.parent = parent;
    }

    @Override
    public abstract boolean matches(ISegment event);

    @Override
    public List<String> getValidChildren() {
            return Arrays.asList(VALID_CHILDREN);
    }

    @Override
    public @Nullable ITmfFilterTreeNode clone() {
        try {
            TmfFilterTreeNode clone = (TmfFilterTreeNode) super.clone();
            clone.parent = null;
            clone.children = new ArrayList<>(children.size());
            for (ITmfFilterTreeNode child : getChildren()) {
                clone.addChild(child.clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }
}

