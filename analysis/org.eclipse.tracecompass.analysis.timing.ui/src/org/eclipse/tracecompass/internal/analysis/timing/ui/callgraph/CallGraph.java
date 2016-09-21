/******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.flamegraph.Messages;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;

/**
 * This view displays a call graph
 *
 * @author Sonia Farrah
 */
public class CallGraph extends ViewPart {

    private final class CallGraphPopulator extends Job {
        private final CallGraphAnalysis fFlamegraphModule;

        public CallGraphPopulator(String name, CallGraphAnalysis flamegraphModule) {
            super(name);
            fFlamegraphModule = flamegraphModule;
        }

        /**
         * Get the event's symbol.It could be an address or a name.
         *
         * @param fGEvent
         *            An event
         * @param symbolProvider
         *            A symbol provider
         */
        private String getFuntionSymbol(CallGraphNode event, ISymbolProvider symbolProvider) {
            String funcSymbol = ""; //$NON-NLS-1$
            if (event.getSymbol() instanceof Long || event.getSymbol() instanceof Integer) {
                long longAddress = ((Long) event.getSymbol()).longValue();
                funcSymbol = symbolProvider.getSymbolText(event.getPid(), (long) (fTrace.getStartTime().toNanos() * 0.5 + fTrace.getEndTime().toNanos() * 0.5), longAddress);
                if (funcSymbol == null) {
                    return "0x" + Long.toHexString(longAddress); //$NON-NLS-1$
                }
            } else {
                return event.getSymbol().toString();
            }
            return funcSymbol;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            fFlamegraphModule.waitForCompletion(monitor);
            List<@NonNull Map<@NonNull Long, @NonNull CallGraphNode>> callGraphThreads = fFlamegraphModule.getCallGraphThreads();
            if (callGraphThreads.isEmpty()) {
                return Status.OK_STATUS;
            }
                Map<@NonNull Long, @NonNull CallGraphNode> list = callGraphThreads.get(0);
            Display.getDefault().syncExec(() -> {
                long totaltime = 0;
                Collection<@NonNull CallGraphNode> values = list.values();
                for (GraphNode graphNode : fNodes.values()) {
                    graphNode.dispose();
                }
                fNodes.clear();
                for (CallGraphNode function : values) {
                    GraphNode graphNode = new GraphNode(fGraph, SWT.NONE, getFuntionSymbol(function, SymbolProviderManager.getInstance().getSymbolProvider(fTrace)));
                    graphNode.setData(function);
                    fNodes.put(function, graphNode);
                    if (function.getCallers().isEmpty()) {
                        totaltime = function.getDuration();
                    }
                }
                for (CallGraphNode function : values) {
                    GraphNode functionNode = fNodes.get(function);
                    final Map<@NonNull CallGraphNode, @NonNull Integer> children = function.getChildren();
                    for (CallGraphNode entry : children.keySet()) {
                        GraphNode child = fNodes.get(entry);
                        GraphConnection cnnx = new GraphConnection(fGraph, ZestStyles.CONNECTIONS_DIRECTED, functionNode, child);
                        cnnx.setText(String.valueOf(children.get(entry)));
                        if (function.getCallers().isEmpty()) {
                            cnnx.setWeight(1.0);
                        }
                        cnnx.setLineWidth(Math.max(1, (int) (5.0 * entry.getDuration() / totaltime)));
                    }
                }
                fGraph.applyLayout();

            });

            return Status.OK_STATUS;
        }
    }

    /**
     * ID
     */
    public static final @NonNull String ID = CallGraph.class.getPackage().getName() + ".cgraph"; //$NON-NLS-1$
    private Map<CallGraphNode, GraphNode> fNodes = new HashMap<>();
    private ITmfTrace fTrace;
    private Graph fGraph;

    /**
     * Constructor
     */
    public CallGraph() {
        super();
        TmfSignalManager.register(this);
    }

    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
        super.dispose();
    }

    @Override

    public void createPartControl(Composite parent) {

        fGraph = new Graph(parent, SWT.NONE);

        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
        fGraph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
        fGraph.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println(e);
            }

        });
    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        if (fTrace != signal.getTrace()) {
            fTrace = signal.getTrace();
            buildCallGraph();
        }
    }

    /**
     * Signal updated
     *
     * @param sig
     *            signal
     */
    @TmfSignalHandler
    public void selectionUpdated(TmfSelectionRangeUpdatedSignal sig) {
        // graph.dispose();
        fTrace = TmfTraceManager.getInstance().getActiveTrace();
        buildCallGraph();
    }

    /**
     * Handler for the trace opened signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void TraceOpened(TmfTraceOpenedSignal signal) {
        fTrace = signal.getTrace();
        buildCallGraph();
    }

    private void buildCallGraph() {
        if (fTrace != null) {
            CallGraphAnalysis flamegraphModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, CallGraphAnalysis.class, CallGraphAnalysisUI.ID);
            if (flamegraphModule == null) {
                return;
            }
            // flamegraphModule.addListener(fListener);
            flamegraphModule.schedule();
            Job j = new CallGraphPopulator(Messages.CallGraphAnalysis_Execution, flamegraphModule);
            j.schedule();
        }
    }

    @Override
    public void setFocus() {
        // do nothing
    }

}
