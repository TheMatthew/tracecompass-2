/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * Filter node for the event match operation
 *
 * @version 1.0
 * @author Patrick Tasse
 * @since 1.1
 */
public class TmfFilterNode extends TmfFilterTreeNode {

    /** filter node name */
    public static final String NODE_NAME = "FILTER"; //$NON-NLS-1$
    /** name attribute name */
    public static final String NAME_ATTR = "name"; //$NON-NLS-1$

    private boolean fIsActive;

    @Nullable String fFilterName;

    /**
     * @param filterName the filter name
     */
    public TmfFilterNode(@Nullable String filterName) {
        super(null);
        fFilterName = filterName;
    }

    /**
     * @param parent the parent node
     * @param filterName the filter name
     */
    public TmfFilterNode(ITmfFilterTreeNode parent, String filterName) {
        super(parent);
        fFilterName = filterName;
    }

    /**
     * @return the filer name
     */
    public @Nullable String getFilterName() {
        return fFilterName;
    }

    /**
     * @param filterName the filer name
     */
    public void setFilterName(String filterName) {
        fFilterName = filterName;
    }

    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public List<String> getValidChildren() {
        if (getChildrenCount() == 0) {
            return super.getValidChildren();
        }
        return new ArrayList<>(0); // only one child allowed
    }

    @Override
    public String toString(boolean explicit) {
        StringBuffer buf = new StringBuffer();
        buf.append(fFilterName);
        buf.append(": "); //$NON-NLS-1$
        if (getChildrenCount() > 1) {
            buf.append("( "); //$NON-NLS-1$
        }
        for (int i = 0; i < getChildrenCount(); i++) {
            ITmfFilterTreeNode node = getChildren()[i];
            buf.append(node.toString(explicit));
            if (i < (getChildrenCount() - 1)) {
                buf.append(" and "); //$NON-NLS-1$
            }
        }
        if (getChildrenCount() > 1) {
            buf.append(" )"); //$NON-NLS-1$
        }
        return buf.toString();
    }

    @Override
    public boolean matches(ISegment segment) {
        // There should be at most one child
        for (ITmfFilterTreeNode node : getChildren()) {
            if (node.matches(segment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isActive() {
        return fIsActive;
    }

    @Override
    public void setActive(boolean isActive) {
        fIsActive = isActive;
    }
}
