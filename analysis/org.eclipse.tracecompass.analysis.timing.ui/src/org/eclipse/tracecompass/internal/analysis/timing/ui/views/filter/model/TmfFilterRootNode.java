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
import java.util.Arrays;
import java.util.List;

import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * The Filter tree root node
 *
 * @version 1.0
 * @author Patrick Tasse
 * @since 1.1
 */
public class TmfFilterRootNode extends TmfFilterTreeNode {

    /** root node name */
    public static final String NODE_NAME = "ROOT"; //$NON-NLS-1$

    private static final String[] VALID_CHILDREN = {
        TmfFilterNode.NODE_NAME
    };

    private boolean fIsActive;

    /**
     * Default constructor
     */
    public TmfFilterRootNode() {
        super(null);
    }

    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public List<String> getValidChildren() {
        return Arrays.asList(VALID_CHILDREN);
    }

    @Override
    public String toString(boolean explicit) {
        StringBuffer buf = new StringBuffer("root"); //$NON-NLS-1$
        if (getChildrenCount() > 0) {
            buf.append(' ');
            List<String> strings = new ArrayList<>();
            for (ITmfFilterTreeNode child : getChildren()) {
                strings.add(child.toString(explicit));
            }
            buf.append(strings.toString());
        }
        return buf.toString();
    }

    @Override
    public boolean matches(ISegment event) {
        for (ITmfFilterTreeNode node : getChildren()) {
            if (! node.matches(event)) {
                return false;
            }
        }
        return true;
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

