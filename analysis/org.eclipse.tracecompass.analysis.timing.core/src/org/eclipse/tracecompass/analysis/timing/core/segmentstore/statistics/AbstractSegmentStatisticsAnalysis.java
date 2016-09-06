/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * Abstract analysis to build statistics data for a segment store
 *
 * @author Jean-Christian Kouame
 */
public abstract class AbstractSegmentStatisticsAnalysis extends TmfAbstractAnalysisModule {

    private @Nullable IAnalysisModule fSegmentStoreProviderModule;

    private final SegmentStoreStatistics [] fTotalStats = new SegmentStoreStatistics[2];

    private List<@Nullable Map<String, SegmentStoreStatistics>> fPerSegmentTypeStats = new ArrayList<>(2);

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace != null) {
            ISegmentStoreProvider provider = getSegmentProviderAnalysis(trace);
            if (provider instanceof IAnalysisModule) {
                fSegmentStoreProviderModule = (IAnalysisModule) provider;
                return ImmutableList.of((IAnalysisModule) provider);
            }
        }
        return super.getDependentAnalyses();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        Collection<ISegment> segStore = getSegmentStore();
        return setStats(segStore, false, monitor);
    }

    private boolean setStats(Collection<ISegment> segStore, boolean isSelection, IProgressMonitor monitor) {

        @Nullable SegmentStoreStatistics totalStat = calculateTotalManual(segStore, monitor);

        if (totalStat == null) {
            return false;
        }
        int index = isSelection ? 1 : 0;

        @NonNull Map<@NonNull String, @NonNull SegmentStoreStatistics> perTypeStat = calculateTotalPerType(segStore, monitor);
        if (perTypeStat.isEmpty()) {
            fTotalStats[index] = null;
            fPerSegmentTypeStats.add(index, null);
            return false;
        }
        fTotalStats[index] = totalStat;
        fPerSegmentTypeStats.add(index, perTypeStat);
        return true;
    }

    /**
     * Get the segment store from which we want the statistics
     *
     * @return The segment store
     * @since 2.0
     */
    protected Collection<@NonNull ISegment> getSegmentStore() {
        IAnalysisModule segmentStoreProviderModule = fSegmentStoreProviderModule;
        ITmfTrace trace = getTrace();
        if (!(segmentStoreProviderModule instanceof ISegmentStoreProvider) || (trace == null)) {
            return Collections.EMPTY_LIST;
        }
        segmentStoreProviderModule.waitForCompletion();

        @Nullable ISegmentStore<@NonNull ISegment> segmentStore = ((ISegmentStoreProvider) segmentStoreProviderModule).getSegmentStore();
        return segmentStore != null ? segmentStore.stream().collect(Collectors.toList()) : Collections.EMPTY_LIST;
    }

    private static @Nullable SegmentStoreStatistics calculateTotalManual(Collection<ISegment> segments, IProgressMonitor monitor) {
        SegmentStoreStatistics total = StreamSupport.stream(segments.spliterator(), true).collect(SegmentStoreStatistics::new, SegmentStoreStatistics::update, SegmentStoreStatistics::merge);
        if (monitor.isCanceled()) {
            return null;
        }
        return total;
    }

    private Map<@NonNull String, @NonNull SegmentStoreStatistics> calculateTotalPerType(Collection<ISegment> segments, IProgressMonitor monitor) {
        Map<String, SegmentStoreStatistics> perSegmentTypeStats = new HashMap<>();

        Iterator<ISegment> iter = segments.iterator();
        while (iter.hasNext()) {
            if (monitor.isCanceled()) {
                return Collections.EMPTY_MAP;
            }
            ISegment segment = iter.next();
            String segmentType = getSegmentType(segment);
            if (segmentType != null) {
                SegmentStoreStatistics values = perSegmentTypeStats.get(segmentType);
                if (values == null) {
                    values = new SegmentStoreStatistics();
                }
                values.update(segment);
                perSegmentTypeStats.put(segmentType, values);
            }
        }
        return perSegmentTypeStats;
    }

    /**
     * Get the selection range statistics
     *
     * @param start
     *            The selection start time
     * @param end
     *            The selection end time
     * @param monitor
     *            The progress monitor
     * @return True if the update succeed, false otherwise
     *
     * @since 2.0
     */
    public boolean updateSelectionStats(long start, long end, IProgressMonitor monitor) {
        Collection<@NonNull ISegment> selection = Collections.EMPTY_LIST;
        IAnalysisModule segmentStoreProviderModule = fSegmentStoreProviderModule;
        if (segmentStoreProviderModule != null) {
            ITmfTrace trace = getTrace();
            if (segmentStoreProviderModule instanceof ISegmentStoreProvider && trace != null) {
                segmentStoreProviderModule.waitForCompletion();
                if (monitor.isCanceled()) {
                    return false;
                }
                @Nullable ISegmentStore<@NonNull ISegment> segmentStore = ((ISegmentStoreProvider) segmentStoreProviderModule).getSegmentStore();
                selection = segmentStore != null ? (Collection<@NonNull ISegment>) segmentStore.getIntersectingElements(start, end) : Collections.EMPTY_LIST;
            }
        }
        return setStats(selection, true, monitor);
    }

    /**
     * Get the type of a segment. Statistics per type will use this type as a
     * key
     *
     * @param segment
     *            the segment for which to get the type
     * @return The type of the segment
     */
    protected abstract @Nullable String getSegmentType(ISegment segment);

    /**
     * Find the segment store provider used for this analysis
     *
     * @param trace
     *            The active trace
     *
     * @return The segment store provider
     */
    protected abstract @Nullable ISegmentStoreProvider getSegmentProviderAnalysis(ITmfTrace trace);

    @Override
    protected void canceling() {
    }

    /**
     * The total statistics
     *
     * @param isSelection
     *            Tells whether the statistics requested is for a selection or
     *            the whole trace
     *
     * @return the total statistics
     * @since 2.0
     */
    public @Nullable SegmentStoreStatistics getTotalStats(boolean isSelection) {
        return isSelection ? fTotalStats[1] : fTotalStats[0];
    }

    /**
     * The per syscall statistics
     *
     * @param isSelection
     *            Tells whether the statistics requested is for a selection or
     *            the whole trace
     *
     * @return the per syscall statistics
     * @since 2.0
     */
    public @Nullable Map<String, SegmentStoreStatistics> getPerSegmentTypeStats(boolean isSelection) {
        return isSelection ? fPerSegmentTypeStats.get(1) : fPerSegmentTypeStats.get(0);
    }

}
