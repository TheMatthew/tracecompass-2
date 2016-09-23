/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;

import com.google.common.collect.ImmutableList;

/**
 * Segment store provider. Useful to populate views.
 *
 * @author Matthew Khouzam
 * @since 2.0
 */
public interface ISegmentStoreProvider {

    /**
     * Add a listener for the viewers
     *
     * @param listener
     *            listener for each type of viewer
     */
    void addListener(IAnalysisProgressListener listener);

    /**
     * Remove listener for the viewers
     *
     * @param listener
     *            listener for each type of viewer
     */
    void removeListener(IAnalysisProgressListener listener);

    /**
     * Return the pre-defined set of segment aspects exposed by this analysis.
     *
     * It should not be null, but could be empty.
     *
     * @return The segment aspects for this analysis
     */
    Iterable<ISegmentAspect> getSegmentAspects();

    /**
     * Returns the result in a from the analysis in a ISegmentStore
     *
     * @return Results from the analysis in a ISegmentStore
     */
    @Nullable
    ISegmentStore<ISegment> getSegmentStore();

    /**
     * Get the ID of the segment store provider
     *
     * @return The id
     * @since 2.0
     */
    String getProviderId();

    /**
     * Base segments aspects
     * @since 2.0
     */
    static List<ISegmentAspect> BASED_SEGMENT_ASPECTS = ImmutableList.of(StartAspect.INSTANCE, EndAspect.INSTANCE,
            DurationAspect.INSTANCE);
    /**
     * Start aspects
     *
     * @author Jean-Christian Kouame
     * @since 2.0
     *
     */
    final class StartAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new StartAspect();

        private StartAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull("Start Time"); //$NON-NLS-1$
        }

        @Override
        public String getName() {
            return checkNotNull("Start Time"); //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return SegmentComparators.INTERVAL_START_COMPARATOR;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return TmfTimestampFormat.getDefaulTimeFormat().format(segment.getStart());
        }
    }

    /**
     * End aspects
     *
     * @author Jean-Christian Kouame
     * @since 2.0
     *
     */
    final class EndAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new EndAspect();

        private EndAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull("End Time"); //$NON-NLS-1$
        }

        @Override
        public String getName() {
            return checkNotNull("End Time"); //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return SegmentComparators.INTERVAL_END_COMPARATOR;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return TmfTimestampFormat.getDefaulTimeFormat().format(segment.getEnd());
        }
    }

    /**
     * Duration aspects
     *
     * @author Jean-Christian Kouame
     * @since 2.0
     */
    final class DurationAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new DurationAspect();
        private static final Format DECIMAL_FORMAT = new DecimalFormat("###,###.##"); //$NON-NLS-1$

        private DurationAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull("Duration"); //$NON-NLS-1$
        }

        @Override
        public String getName() {
            return checkNotNull("Duration"); //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return SegmentComparators.INTERVAL_LENGTH_COMPARATOR;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return DECIMAL_FORMAT.format(segment.getLength());
        }
    }
}