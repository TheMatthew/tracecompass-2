/*******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   France Lapointe Nguyen - Initial API and implementation
 *   Bernd Hufmann - Move abstract class to TMF
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.dialog.LatencyViewFilterDialog;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.ExportToTsvAction;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.ui.IActionBars;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

/**
 * View for displaying a segment store analysis in a table.
 *
 * @author France Lapointe Nguyen
 * @since 2.0
 */
public abstract class AbstractSegmentStoreTableView extends TmfView {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final Action fExportAction = new ExportToTsvAction() {
        @Override
        protected void exportToTsv(@Nullable OutputStream stream) {
            AbstractSegmentStoreTableView.this.exportToTsv(stream);

        }

        @Override
        protected @Nullable Shell getShell() {
            return getViewSite().getShell();
        }
    };

    private @Nullable AbstractSegmentStoreTableViewer fSegmentStoreViewer;
    private @Nullable Action fFilter;
    private @Nullable Composite fParent;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     */
    public AbstractSegmentStoreTableView() {
        super(""); //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(@Nullable Composite parent) {
        fParent = parent;
        SashForm sf = new SashForm(parent, SWT.NONE);
        TableViewer tableViewer = new TableViewer(sf, SWT.FULL_SELECTION | SWT.VIRTUAL);

        fSegmentStoreViewer = createSegmentStoreViewer(tableViewer);
        getViewSite().getActionBars().getMenuManager().add(fExportAction);
        setInitialData();

        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(NonNullUtils.checkNotNull(bars.getToolBarManager()));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(createFilterActions());
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    private @Nullable Action createFilterActions() {
        if (fFilter == null) {
            fFilter = new Action() {
                @Override
                public void run() {
                    showFilterDialog();
                }
                @Override
                public ImageDescriptor getImageDescriptor() {
                    // TODO Auto-generated method stub
                    return NonNullUtils.checkNotNull(Activator.getDefault().getImageDescripterFromPath("/icons/elcl16/filters_view.gif")); //$NON-NLS-1$
                }
            };
        }
        return fFilter;
    }

    private void showFilterDialog() {
        final Composite parent = fParent;
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        final @Nullable AbstractSegmentStoreTableViewer viewer = fSegmentStoreViewer;
        if (trace != null && parent != null && viewer != null && !parent.isDisposed()) {
            @Nullable ISegmentStoreProvider segmentProvider = viewer.getSegmentProvider();
            if (segmentProvider != null) {
                LatencyViewFilterDialog dialog = new LatencyViewFilterDialog(NonNullUtils.checkNotNull(parent.getShell()), trace, viewer.getColumnsAspects(), segmentProvider.getProviderId());
                dialog.open();
            }
        }
    }

    @Override
    public void setFocus() {
        if (fSegmentStoreViewer != null) {
            fSegmentStoreViewer.getTableViewer().getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fSegmentStoreViewer != null) {
            fSegmentStoreViewer.dispose();
        }
    }

    /**
     * Returns the latency analysis table viewer instance
     *
     * @param tableViewer
     *            the table viewer to use
     * @return the latency analysis table viewer instance
     */
    protected abstract AbstractSegmentStoreTableViewer createSegmentStoreViewer(TableViewer tableViewer);

    /**
     * Get the table viewer
     *
     * @return the table viewer, useful for testing
     */
    @Nullable
    public AbstractSegmentStoreTableViewer getSegmentStoreViewer() {
        return fSegmentStoreViewer;
    }

    /**
     * Set initial data into the viewer
     */
    private void setInitialData() {
        if (fSegmentStoreViewer != null) {
            @NonNull AbstractSegmentStoreTableViewer segmentStoreViewer = fSegmentStoreViewer;
            @Nullable ISegmentStoreProvider segmentProvider = segmentStoreViewer.getSegmentProvider();
            segmentStoreViewer.setData(segmentProvider);
        }
    }

    /**
     * Export a given items's TSV
     *
     * @param stream
     *            an output stream to write the TSV to
     * @since 2.0
     */
    @VisibleForTesting
    protected void exportToTsv(@Nullable OutputStream stream) {
        try (PrintWriter pw = new PrintWriter(stream)) {
            AbstractSegmentStoreTableViewer segmentStoreViewer = getSegmentStoreViewer();
            if (segmentStoreViewer == null) {
                return;
            }
            Table table = segmentStoreViewer.getTableViewer().getTable();
            int size = table.getItemCount();
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn column = table.getColumn(i);
                if (column == null) {
                    return;
                }
                String columnName = String.valueOf(column.getText());
                if (columnName.isEmpty() && i == table.getColumnCount() - 1) {
                    // Linux GTK2 undocumented feature
                    break;
                }
                columns.add(columnName);
            }
            pw.println(Joiner.on('\t').join(columns));
            for (int i = 0; i < size; i++) {
                TableItem item = table.getItem(i);
                if (item == null) {
                    continue;
                }
                List<String> data = new ArrayList<>();
                for (int col = 0; col < columns.size(); col++) {
                    data.add(String.valueOf(item.getText(col)));
                }
                pw.println(Joiner.on('\t').join(data));
            }
        }
    }
}
