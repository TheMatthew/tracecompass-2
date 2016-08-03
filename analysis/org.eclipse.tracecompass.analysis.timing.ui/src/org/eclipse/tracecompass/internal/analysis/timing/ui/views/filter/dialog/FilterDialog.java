/*******************************************************************************
 * Copyright (c) 2010, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.dialog;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterNode;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

/**
 * The dialog for user-defined filters.
 *
 * @version 1.0
 * @author Patrick Tasse
 * @since 1.1
 */
public class FilterDialog extends Dialog {

    @Nullable TmfFilterNode fRoot;
    @Nullable FilterViewer fViewer;
    List<ISegmentAspect> fAspects;

    /**
     * Constructor.
     *
     * @param shell
     *            The shell to which this dialog is attached
     * @param list
     */
    public FilterDialog(Shell shell, @NonNull List<@NonNull ISegmentAspect> aspects) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        fAspects = aspects;
    }

    @Override
    protected Control createDialogArea(@Nullable Composite parent) {
        getShell().setText(Messages.FilterDialog_FilterDialogTitle);
        getShell().setMinimumSize(getShell().computeSize(550, 250));
        Composite composite = (Composite) super.createDialogArea(parent);

        fViewer = new FilterViewer(composite, SWT.BORDER, true, fAspects);
        fViewer.setInput(NonNullUtils.checkNotNull(fRoot));
        return composite;
    }

    /**
     * @param filter
     *            the filter to set
     */
    public void setFilter(@Nullable ITmfFilterTreeNode filter) {
        TmfFilterNode root = new TmfFilterNode(null);
        if (filter != null) {
            root.addChild(filter.clone());
        }
        if (fViewer != null) {
            fViewer.setInput(root);
        }
        fRoot = root;
    }

    /**
     * @return the filter
     */
    public @Nullable ITmfFilterTreeNode getFilter() {
        @Nullable TmfFilterNode root = fRoot;
        if (root != null && root.hasChildren()) {
            return NonNullUtils.checkNotNull(root).getChild(0).clone();
        }
        return null;
    }

}
