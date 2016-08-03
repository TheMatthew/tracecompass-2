/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.dialog;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.FilterManager;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilter;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.signal.TmfSegmentFilterAppliedSignal;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * The segment filter dialog
 *
 * @author Jean-Christian Kouame
 * @since 1.1
 */
public class LatencyViewFilterDialog extends Dialog {

    private static @Nullable CheckboxTableViewer fFiltersTable;
    private static boolean fMatchAny;
    private static List<@NonNull ISegmentAspect> fAspects;
    private static String fSegmentProviderId;
    private static ITmfTrace fTrace;
    private static Map<String, ISegmentFilter> fFilters = new HashMap<>();

    /**
     * Constructor
     *
     * @param parentShell
     *            The parent shell
     * @param trace
     *            The current trace
     * @param aspects
     *            The list of applicable aspects
     * @param segmentProviderId
     *            The segment store provider Id
     */
    public LatencyViewFilterDialog(Shell parentShell, ITmfTrace trace, List<@NonNull ISegmentAspect> aspects, String segmentProviderId) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.MAX | getShellStyle());
        fAspects = aspects;
        fSegmentProviderId = segmentProviderId;
        fTrace = trace;
        loadFilters();
    }

    @Override
    public int open() {
        if (super.open() == 0) {
            storeFilters();
        }
        return 0;
    }



    private void storeFilters() {
        SegmentFilter segmentFilter = createFilterNode();
        FilterManager.getInstance().setSegmentFilter(segmentFilter);
        TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
        if (ctx != null) {
            ctx.setData(ISegmentFilter.FILTER_PREFIX + "." + fSegmentProviderId, segmentFilter); //$NON-NLS-1$
        }
        TmfSignalManager.dispatchSignal(new TmfSegmentFilterAppliedSignal(fTrace, this, segmentFilter));
    }

    private static @NonNull SegmentFilter createFilterNode() {
        SegmentFilter segmentFilter = new SegmentFilter(null);
        segmentFilter.setSegmentProviderId(fSegmentProviderId);
        segmentFilter.setMatchAny(fMatchAny);
        segmentFilter.setChildren(fFilters.values());
        return segmentFilter;
    }

    private static void loadFilters() {
        fFilters.clear();
        if (!load2()) {
            TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
            if (ctx != null) {
                Object o = ctx.getData(ISegmentFilter.FILTER_PREFIX + "." + fSegmentProviderId); //$NON-NLS-1$
                if (o instanceof SegmentFilter) {
                    SegmentFilter filter = (SegmentFilter) o;
                    for (Object f : filter.getChildren()) {
                        if (f instanceof ISegmentFilter) {
                            fFilters.put(((ISegmentFilter) f).toString(), (ISegmentFilter) f);
                        }
                    }
                    fMatchAny = filter.isMatchAny();
                }
            }
            updateFiltersTable();
        }
    }

    private static boolean load2() {
        SegmentFilter segmentFilter = FilterManager.getInstance().getSegmentFilter(fSegmentProviderId);
        if (segmentFilter != null) {
            for (ISegmentFilter filter : segmentFilter.getChildren()) {
                fFilters.put(filter.toString(), filter);
            }
        }
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(@Nullable Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected Control createDialogArea(@Nullable Composite parent) {
        Composite dialog = checkNotNull((Composite) super.createDialogArea(parent));
        Composite area = new Composite(dialog, SWT.NONE);
        area.setLayout(new GridLayout(5, false));
        area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        createFilterList(area);

        return dialog;
    }

    private static void createFilterList(Composite parent) {

        Button radioAny = new Button(parent, SWT.BORDER | SWT.RADIO);
        radioAny.setText("Match Any");
        Button radioAll = new Button(parent, SWT.BORDER | SWT.RADIO);
        radioAll.setText("Match All");
        fMatchAny = true;
        radioAny.setSelection(fMatchAny);
        radioAny.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                fMatchAny = radioAny.getSelection();
            }

            @Override
            public void widgetDefaultSelected(@Nullable SelectionEvent e) {
                // Do nothing
            }
        });
        new Label(parent, SWT.NONE);
        new Label(parent, SWT.NONE);
        new Label(parent, SWT.NONE);

        Composite tableParent = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
        gd.heightHint = 100;
        gd.minimumWidth = 400;
        tableParent.setLayoutData(gd);
        tableParent.setLayout(new GridLayout());

        CheckboxTableViewer table = CheckboxTableViewer.newCheckList(tableParent, SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.SINGLE);
        table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
        table.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(@Nullable CheckStateChangedEvent event) {
                final @NonNull CheckStateChangedEvent e = checkNotNull(event);
                @Nullable ISegmentFilter filter = checkNotNull(fFilters).get(e.getElement().toString());
                if (filter != null) {
                    filter.setActive(e.getChecked());
                }
            }
        });
        fFiltersTable = table;
        updateFiltersTable();

        Composite optionsComposite = new Composite(parent, SWT.NONE);
        optionsComposite.setLayout(new GridLayout());

        Button add = new Button(optionsComposite, SWT.BORDER | SWT.PUSH);
        gd = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
        add.setLayoutData(gd);

        add.setText("Add");
        add.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                if (!optionsComposite.isDisposed()) {
                    FilterDialog dialog = new FilterDialog(optionsComposite.getShell(), checkNotNull(fAspects));
                    dialog.setFilter(null);
                    dialog.open();

                    if (dialog.getReturnCode() == Window.OK) {
                        @Nullable ITmfFilterTreeNode filter = dialog.getFilter();
                        if (filter != null) {
                            checkNotNull(fFilters).put(filter.toString(), filter);
                            updateFiltersTable();
                        }
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(@Nullable SelectionEvent e) {
                // TODO Auto-generated method stub

            }
        });

        Button rm = new Button(optionsComposite, SWT.BORDER | SWT.PUSH);
        gd = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
        rm.setLayoutData(gd);

        rm.setText("Remove");
        rm.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                final ISelection selection = table.getSelection();
                if (!selection.isEmpty()) {
                    checkNotNull(fFilters).remove(((StructuredSelection) selection).getFirstElement());
                    updateFiltersTable();
                }
            }

            @Override
            public void widgetDefaultSelected(@Nullable SelectionEvent e) {
                // TODO Auto-generated method stub

            }
        });
    }

    private static void updateFiltersTable() {
        Display.getCurrent().asyncExec(new Runnable() {

            @Override
            public void run() {
                CheckboxTableViewer list = fFiltersTable;
                if (list == null) {
                    return;
                }
                list.getTable().removeAll();
                for (Map.Entry<String, org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter> entry : checkNotNull(fFilters).entrySet()) {
                    final @NonNull ISegmentFilter value = checkNotNull(entry.getValue());
                    list.add(value.toString());
                    list.setChecked(value.toString(), value.isActive());
                }
            }
        });
    }

    /**
     * Get the type of the search
     *
     * @return True if it is a 'match any' search, false if it is a 'match all'
     *         search
     */
    public static boolean isMatchAny() {
        return fMatchAny;
    }

//    /**
//     * private class to represent the general filter in the filter table list
//     *
//     * @author Jean-Christian Kouame
//     */
//    private static final class GeneralFilter {
//        @NonNull protected List<ISegmentFilter> filters;
//        protected boolean matchAny;
//
//        public GeneralFilter(@NonNull Collection<ISegmentFilter> filters, boolean matchAny) {
//            this.filters = new ArrayList<>(filters);
//            this.matchAny = matchAny;
//        }
//    }
}
