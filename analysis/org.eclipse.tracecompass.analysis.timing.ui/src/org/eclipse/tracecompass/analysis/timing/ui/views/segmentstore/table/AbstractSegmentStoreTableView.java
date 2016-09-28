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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

import com.google.common.base.Joiner;

/**
 * View for displaying a segment store analysis in a table.
 *
 * @author France Lapointe Nguyen
 * @since 2.0
 */
public abstract class AbstractSegmentStoreTableView extends TmfView {

    private final Action fExportAction = new Action() {
        private final String[] fExtensions = { "*.tsv", "*.*" };//$NON-NLS-1$//$NON-NLS-2$

        @Override
        public void run() {
            AbstractSegmentStoreTableViewer segmentStoreViewer = getSegmentStoreViewer();
            if (segmentStoreViewer == null)
                return;
            FileDialog fd = new FileDialog(getViewSite().getShell());
            fd.setFilterExtensions(fExtensions);
            String fileName = fd.open();
            try (PrintWriter pw = new PrintWriter(fileName)) {
                Table table = segmentStoreViewer.getTableViewer().getTable();
                int size = table.getItemCount();
                List<String> columns = new ArrayList<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    columns.add(table.getColumn(i).getText());
                }
                pw.println(Joiner.on('\t').join(columns));
                for (int i = 0; i < size; i++) {
                    TableItem item = table.getItem(i);
                    List<String> data = new ArrayList<>();
                    for (int col = 0; col < columns.size(); col++) {
                        data.add(item.getText(col));
                    }
                    pw.println(Joiner.on('\t').join(data));
                }
            } catch (FileNotFoundException e) {

            }
        }

        @Override
        public String getText() {
            return "Export to TSV";
        }
    };

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private @Nullable AbstractSegmentStoreTableViewer fSegmentStoreViewer;

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
        SashForm sf = new SashForm(parent, SWT.NONE);
        TableViewer tableViewer = new TableViewer(sf, SWT.FULL_SELECTION | SWT.VIRTUAL);
        fSegmentStoreViewer = createSegmentStoreViewer(tableViewer);
        getViewSite().getActionBars().getMenuManager().add(fExportAction);
        setInitialData();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

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
            fSegmentStoreViewer.setData(fSegmentStoreViewer.getSegmentProvider());
        }
    }
}
