/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.statistics;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.SegmentStoreStatistics;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.CallGraphStatisticsAnalysisUI;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

/**
 * A tree viewer implementation for displaying latency statistics
 *
 * @author Bernd Hufmann
 *
 */
public class CallGraphStatisticsViewer extends AbstractSegmentStoreStatisticsViewer {

    private static final String SYSCALL_LEVEL = checkNotNull(Messages.CallgraphStatistics_FunctionName);

    private class FunctionEntry extends SegmentStoreStatisticsEntry {

        public FunctionEntry(String key, SegmentStoreStatistics value) {
            super(key, value);
        }

        @Override
        public @Nullable String getName() {
            String original = super.getName();
            ITmfTrace trace = getTrace();
            if (trace != null) {
                try {
                    Long address = Long.parseLong(original, 10);
                    String res = SymbolProviderManager.getInstance().getSymbolProvider(trace).getSymbolText(address);
                    if (res != null) {
                        return res;
                    }
                    return "0x" + Long.toHexString(address); //$NON-NLS-1$
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            return original;
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     */
    public CallGraphStatisticsViewer(Composite parent) {
        super(parent);
    }

    /**
     * Gets the statistics analysis module
     *
     * @return the statistics analysis module
     */
    @Override
    protected @Nullable TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
        return new CallGraphStatisticsAnalysisUI();
    }

    @Override
    protected @Nullable ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }

        TmfAbstractAnalysisModule analysisModule = getStatisticsAnalysisModule();

        if (getTrace() == null || !(analysisModule instanceof CallGraphStatisticsAnalysis)) {
            return null;
        }

        CallGraphStatisticsAnalysis module = (CallGraphStatisticsAnalysis) analysisModule;

        module.waitForCompletion();

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        final SegmentStoreStatistics entry = module.getTotalStats();
        if (entry != null) {

            List<ITmfTreeViewerEntry> entryList = root.getChildren();

            TmfTreeViewerEntry child = new SegmentStoreStatisticsEntry(checkNotNull(Messages.CallgraphStatistics_TotalLabel), entry);
            entryList.add(child);
            HiddenTreeViewerEntry functions = new HiddenTreeViewerEntry(SYSCALL_LEVEL);
            child.addChild(functions);

            Map<String, SegmentStoreStatistics> perSyscallStats = module.getPerSegmentTypeStats();
            if (perSyscallStats != null) {
                for (Entry<String, SegmentStoreStatistics> statsEntry : perSyscallStats.entrySet()) {
                    functions.addChild(new FunctionEntry(statsEntry.getKey(), statsEntry.getValue()));
                }
            }
        }
        return root;
    }

}
