/*******************************************************************************
 * Copyright (c) 2010, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Yuriy Vashchuk - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * This is the Content Provider of our tree
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @since 1.1
 */
public class FilterTreeContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public void inputChanged(@Nullable Viewer viewer, @Nullable Object oldInput, @Nullable Object newInput) {
        // TODO Auto-generated method stub
    }

    @Override
    public @Nullable Object @Nullable [] getElements(@Nullable Object inputElement) {
        if (inputElement instanceof ITmfFilterTreeNode) {
            ArrayList<ITmfFilterTreeNode> result = new ArrayList<>();
            for (int i = 0; i < ((ITmfFilterTreeNode) inputElement).getChildrenCount(); i++) {
                result.add(((ITmfFilterTreeNode) inputElement).getChild(i));
            }

            return result.toArray();
        }
        return null;
    }

    @Override
    public Object[] getChildren(@Nullable Object parentElement) {
        ArrayList<ITmfFilterTreeNode> result = new ArrayList<>();
        for (int i = 0; i < ((ITmfFilterTreeNode) NonNullUtils.checkNotNull(parentElement)).getChildrenCount(); i++) {
            result.add(((ITmfFilterTreeNode) NonNullUtils.checkNotNull(parentElement)).getChild(i));
        }
        return result.toArray();
    }

    @Override
    public Object getParent(@Nullable Object element) {
        return NonNullUtils.checkNotNull(((ITmfFilterTreeNode) NonNullUtils.checkNotNull(element)).getParent());
    }

    @Override
    public boolean hasChildren(@Nullable Object element) {
        return ((ITmfFilterTreeNode) NonNullUtils.checkNotNull(element)).hasChildren();
    }

}
