/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;

import com.google.common.collect.ImmutableList;

/**
 * CallGraph Analysis with aspects
 *
 * @author Sonia Farrah
 */
public class CallGraphAnalysisUI extends CallGraphAnalysis {

    /**
     * ID
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.callgraphanalysis"; //$NON-NLS-1$
    private static final Format DECIMAL_FORMAT = new DecimalFormat("###,###.##"); //$NON-NLS-1$

    /**
     * Default constructor
     */
    public CallGraphAnalysisUI() {
        super();
    }

    @Override
    public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
        return ImmutableList.of(StartAspect.INSTANCE, EndAspect.INSTANCE,
                DurationAspect.INSTANCE, SymbolAspect.SYMBOL_ASPECT);
    }

    private static final class StartAspect implements ISegmentAspect {
        public static final @NonNull ISegmentAspect INSTANCE = new StartAspect();

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

    private static final class EndAspect implements ISegmentAspect {
        public static final @NonNull ISegmentAspect INSTANCE = new EndAspect();

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

    private static final class DurationAspect implements ISegmentAspect {
        public static final @NonNull ISegmentAspect INSTANCE = new DurationAspect();

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
