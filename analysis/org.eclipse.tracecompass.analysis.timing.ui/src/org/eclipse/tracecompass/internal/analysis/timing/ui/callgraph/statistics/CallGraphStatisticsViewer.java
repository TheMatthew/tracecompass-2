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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.SegmentStoreStatistics;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.CallGraphStatisticsAnalysisUI;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
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

    private static final String FUNTION_NAME = checkNotNull(Messages.CallgraphStatistics_FunctionName);

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
                    ISymbolProvider symbolProvider = SymbolProviderManager.getInstance().getSymbolProvider(trace);
                    ISegment maxSegment = getEntry().getMaxSegment();
                    if (maxSegment instanceof ICalledFunction) {
                        ICalledFunction iCalledFunction = (ICalledFunction) maxSegment;
                        String res = symbolProvider.getSymbolText(iCalledFunction.getProcessId(), iCalledFunction.getStart(), address);
                        if (res != null) {
                            return res;
                        }
                    } else {
                        String res = symbolProvider.getSymbolText(address);
                        if (res != null) {
                            return res;
                        }
                    }

                    return "0x" + Long.toHexString(address); //$NON-NLS-1$
                } catch (NumberFormatException e) {
                    // that's fine
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
    protected @NonNull final String getTypeLabel() {
        return FUNTION_NAME;
    }

    @Override
    protected void setStats(List<ITmfTreeViewerEntry> entryList, AbstractSegmentStatisticsAnalysis module, String rootName) {
        boolean isSelection = !rootName.equals(getTotalLabel()) ? true : false;
        final SegmentStoreStatistics entry = module.getTotalStats(isSelection);
        if (entry != null) {

            if (entry.getNbSegments() == 0) {
                return;
            }
            TmfTreeViewerEntry child = new FunctionEntry(checkNotNull(rootName), entry);
            entryList.add(child);

            final Map<@NonNull String, @NonNull SegmentStoreStatistics> perTypeStats = module.getPerSegmentTypeStats(isSelection);
            if (perTypeStats != null) {
                for (Entry<@NonNull String, @NonNull SegmentStoreStatistics> statsEntry : perTypeStats.entrySet()) {
                    child.addChild(new FunctionEntry(statsEntry.getKey(), statsEntry.getValue()));
                }
            }
        }
    }
}
