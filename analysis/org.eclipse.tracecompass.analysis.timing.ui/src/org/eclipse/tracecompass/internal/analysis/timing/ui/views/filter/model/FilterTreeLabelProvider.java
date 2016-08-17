/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Yuriy Vashchuk - Initial API and implementation
 *   Patrick Tasse - Update filter nodes
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;

/**
 * This is the Label Provider for our Filter Tree
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @since 1.1
 */
public class FilterTreeLabelProvider implements ILabelProvider {

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final String QUOTE = "\""; //$NON-NLS-1$
    private static final String SPACE_QUOTE = " \""; //$NON-NLS-1$
    private static final String NOT = "NOT "; //$NON-NLS-1$

    @Override
    public void addListener(@Nullable ILabelProviderListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isLabelProperty(@Nullable Object element, @Nullable String property) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeListener(@Nullable ILabelProviderListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public @Nullable Image getImage(@Nullable Object element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getText(@Nullable Object element) {
        StringBuilder label = new StringBuilder();

        if (element instanceof TmfFilterNode) {

            TmfFilterNode node = (TmfFilterNode) element;
            label.append(node.getNodeName()).append(' ').append(node.getFilterName() != null &&
                    !NonNullUtils.checkNotNull(node.getFilterName()).isEmpty() ? node.getFilterName() : Messages.FilterTreeLabelProvider_FilterNameHint);

        } else if (element instanceof TmfFilterAndNode) {

            TmfFilterAndNode node = (TmfFilterAndNode) element;
            label.append((node.isNot() ? NOT : EMPTY_STRING)).append(node.getNodeName());

        } else if (element instanceof TmfFilterOrNode) {

            TmfFilterOrNode node = (TmfFilterOrNode) element;
            label.append(node.isNot() ? NOT : EMPTY_STRING).append(node.getNodeName());

        }else if (element instanceof SegmentFilterMatchesNode) {

            SegmentFilterMatchesNode node = (SegmentFilterMatchesNode) element;
            label.append(node.isNot() ? NOT : EMPTY_STRING)
            .append(node.getEventAspect() != null ? node.getAspectLabel(false) : Messages.FilterTreeLabelProvider_AspectHint)
            .append(' ').append(node.getNodeName())
            .append(new StringBuilder().append(SPACE_QUOTE).append(node.getRegex()).append(QUOTE).toString());

        }
        return label.toString();
    }

}