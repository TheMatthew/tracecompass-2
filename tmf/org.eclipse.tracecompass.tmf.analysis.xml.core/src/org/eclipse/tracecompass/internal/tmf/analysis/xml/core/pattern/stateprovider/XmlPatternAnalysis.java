/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.pattern.stateprovider;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.table.Messages;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.model.TmfXmlPatternSegmentBuilder;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.segment.TmfXmlPatternCompositeSegment;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.segment.TmfXmlPatternSegment;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfAnalysisModuleWithStateSystems;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.collect.ImmutableList;

/**
 * Analysis module for pattern matching within traces. This module creates two
 * sub-analyses : A state system analysis that will execute the pattern on the
 * trace and a segment store analysis that will build a segment store with the
 * segments generated by the state system analysis.
 *
 * @author Jean-Christian Kouame
 */
public class XmlPatternAnalysis extends TmfAbstractAnalysisModule implements ITmfAnalysisModuleWithStateSystems, ISegmentStoreProvider {

    /**
     * Segment store supplementary file extension
     */
    public static final @NonNull String SEGMENT_STORE_EXTENSION = ".dat"; //$NON-NLS-1$
    /**
     * state system supplementary file extension
     */
    private static final @NonNull String STATE_SYSTEM_EXTENSION = ".ht"; //$NON-NLS-1$
    private static final String SEGMENT_STORE_SUFFIX = " segment store"; //$NON-NLS-1$
    private static final String STATE_SYSTEM_SUFFIX = " state system"; //$NON-NLS-1$
    private final CountDownLatch fInitialized = new CountDownLatch(1);
    private XmlPatternStateSystemModule fStateSystemModule;
    private XmlPatternSegmentStoreModule fSegmentStoreModule;
    private boolean fInitializationSucceeded;
    private static final Format FORMATTER = new SubSecondTimeWithUnitFormat();
    private static final Format DECIMAL_FORMAT = new DecimalFormat("#.###"); //$NON-NLS-1$

    /**
     * Constructor
     */
    public XmlPatternAnalysis() {
        super();
        fSegmentStoreModule = new XmlPatternSegmentStoreModule(this);
        fStateSystemModule = new XmlPatternStateSystemModule(fSegmentStoreModule);
    }

    @Override
    public @NonNull String getProviderId() {
        return fSegmentStoreModule.getProviderId();
    }

    @Override
    public @Nullable ISegmentStore<@NonNull ISegment> getSegmentStore() {
        return fSegmentStoreModule.getSegmentStore();
    }

    @Override
    public @Nullable ITmfStateSystem getStateSystem(@NonNull String id) {
        return fStateSystemModule.getStateSystem(id);
    }

    @Override
    public @NonNull Iterable<@NonNull ITmfStateSystem> getStateSystems() {
        return fStateSystemModule.getStateSystems();
    }

    @Override
    public boolean waitForInitialization() {
        try {
            fInitialized.await();
        } catch (InterruptedException e) {
            return false;
        }
        return fInitializationSucceeded;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            /* This analysis was cancelled in the meantime */
            analysisReady(false);
            return false;
        }

        File segmentStoreFile = getSupplementaryFile(getSegmentStoreFileName());
        File stateSystemFile = getSupplementaryFile(getStateSystemFileName());
        if (segmentStoreFile == null || stateSystemFile == null) {
            analysisReady(false);
            return false;
        }

        if (!segmentStoreFile.exists()) {
            fStateSystemModule.cancel();
            stateSystemFile.delete();
        }

        IStatus segmentStoreStatus = fSegmentStoreModule.schedule();
        IStatus stateSystemStatus = fStateSystemModule.schedule();
        if (!(segmentStoreStatus.isOK() && stateSystemStatus.isOK())) {
            cancelSubAnalyses();
            analysisReady(false);
            return false;
        }

        /* Wait until the state system module is initialized */
        if (!fStateSystemModule.waitForInitialization()) {
            analysisReady(false);
            cancelSubAnalyses();
            return false;
        }

        ITmfStateSystem stateSystem = fStateSystemModule.getStateSystem();
        if (stateSystem == null) {
            analysisReady(false);
            throw new IllegalStateException("Initialization of the state system module succeeded but the statesystem is null"); //$NON-NLS-1$
        }

        analysisReady(true);

        return fStateSystemModule.waitForCompletion(monitor) && fSegmentStoreModule.waitForCompletion(monitor);
    }

    @Override
    protected void canceling() {
        cancelSubAnalyses();
    }

    private void cancelSubAnalyses() {
        fStateSystemModule.cancel();
        fSegmentStoreModule.cancel();
    }

    @Override
    public void dispose() {
        /*
         * The sub-analyses are not registered to the trace directly, so we need
         * to tell them when the trace is disposed.
         */
        super.dispose();
        fStateSystemModule.dispose();
        fSegmentStoreModule.dispose();
    }

    @Override
    public void setId(@NonNull String id) {
        super.setId(id);
        fStateSystemModule.setId(id);
        fSegmentStoreModule.setId(id);
    }

    @Override
    public void setName(@NonNull String name) {
        super.setName(name);
        fStateSystemModule.setName(name + STATE_SYSTEM_SUFFIX);
        fSegmentStoreModule.setName(name + SEGMENT_STORE_SUFFIX);
    }

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (!super.setTrace(trace)) {
            return false;
        }

        /*
         * Since these sub-analyzes are not built from an extension point, we
         * have to assign the trace ourselves. Very important to do so before
         * calling schedule()!
         */
        return fSegmentStoreModule.setTrace(trace) && fStateSystemModule.setTrace(trace);
    }

    /**
     * Sets the file path of the XML file and the id of pattern analysis in the
     * file
     *
     * @param file
     *            The full path to the XML file
     */
    public void setXmlFile(IPath file) {
        fStateSystemModule.setXmlFile(file);
    }

    /**
     * Make the module available and set whether the initialization succeeded or
     * not. If not, no state system is available and
     * {@link #waitForInitialization()} should return false.
     *
     * @param success
     *            True if the initialization went well, false otherwise
     */
    private void analysisReady(boolean succeeded) {
        fInitializationSucceeded = succeeded;
        fInitialized.countDown();
    }

    private @Nullable File getSupplementaryFile(String filename) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return null;
        }
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        File file = new File(directory + filename);
        return file;
    }

    private String getStateSystemFileName() {
        return fStateSystemModule.getId() + STATE_SYSTEM_EXTENSION;
    }

    private String getSegmentStoreFileName() {
        return fSegmentStoreModule.getId() + SEGMENT_STORE_EXTENSION;
    }

    @Override
    public void addListener(@NonNull IAnalysisProgressListener listener) {
        fSegmentStoreModule.addListener(listener);
    }

    @Override
    public void removeListener(@NonNull IAnalysisProgressListener listener) {
        fSegmentStoreModule.removeListener(listener);
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return buildChainAspects(fStateSystemModule.getProvider(), ImmutableList.of(PatternSegmentNameAspect.INSTANCE, PatternSegmentContentAspect.INSTANCE));
    }

    private @NonNull
    static Iterable<ISegmentAspect> buildChainAspects(XmlPatternStateProvider provider, @NonNull ImmutableList<ISegmentAspect> immutableList) {
        @NonNull List<ISegmentAspect> aspects = new ArrayList<>();
        aspects.addAll(BASED_TABLE_ASPECTS);
        aspects.addAll(immutableList);
        final String prefix = "Time to "; //$NON-NLS-1$
        for (int i = 0; i < provider.getChainStates().size(); i++) {
            String stateId = provider.getChainStates().get(i);
            final int index = i;
            aspects.add(new ISegmentAspect() {

                @Override
                public @Nullable Object resolve(@NonNull ISegment segment) {
                    if (segment instanceof TmfXmlPatternCompositeSegment) {
                        TmfXmlPatternCompositeSegment composite = (TmfXmlPatternCompositeSegment) segment;
                        ISegment subSegment = composite.getSubSegments().get(index);
                        return String.format("%s", FORMATTER.format(subSegment.getLength())) + " (" + DECIMAL_FORMAT.format((100.0 * subSegment.getLength() / segment.getLength())) + "%)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                    return TmfXmlStrings.NULL;
                }

                @Override
                public @NonNull String getName() {
                    return prefix + stateId;
                }

                @Override
                public @NonNull String getHelpText() {
                    return prefix + stateId;
                }

                @Override
                public @Nullable Comparator<?> getComparator() {
                    return new Comparator<ISegment>() {
                        @Override
                        public int compare(@Nullable ISegment o1, @Nullable ISegment o2) {
                            if (o1 == null || o2 == null) {
                                throw new IllegalArgumentException();
                            }
                            if (o1 instanceof TmfXmlPatternCompositeSegment && o2 instanceof TmfXmlPatternCompositeSegment) {
                                TmfXmlPatternCompositeSegment c1 = (TmfXmlPatternCompositeSegment) o1;
                                ISegment ss1 = c1.getSubSegments().get(index);

                                TmfXmlPatternCompositeSegment c2 = (TmfXmlPatternCompositeSegment) o2;
                                ISegment ss2 = c2.getSubSegments().get(index);
                                return Long.compare(ss1.getLength(), ss2.getLength());
                            }
                            return Long.compare(o1.getLength(), o2.getLength());
                        }
                    };
                }
            });
        }
        return aspects;
    }

    /**
     * Get the fsm chain id
     *
     * @return The id
     */
    public String getChainId() {
        XmlPatternStateProvider provider = fStateSystemModule.getProvider();
        return provider != null ? provider.getChainId() : TmfXmlStrings.NULL;
    }

    private static class PatternSegmentNameAspect implements ISegmentAspect {
        public static final @NonNull ISegmentAspect INSTANCE = new PatternSegmentNameAspect();

        private PatternSegmentNameAspect() {}

        @Override
        public String getHelpText() {
            return checkNotNull("Name");
        }
        @Override
        public String getName() {
            return checkNotNull("Name");
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof TmfXmlPatternSegment) {
                return ((TmfXmlPatternSegment) segment).getName()
                        .substring(TmfXmlPatternSegmentBuilder.PATTERN_SEGMENT_NAME_PREFIX.length());
            } else if (segment instanceof TmfXmlPatternCompositeSegment) {
                return ((TmfXmlPatternCompositeSegment) segment).getName()
                        .substring(TmfXmlPatternSegmentBuilder.PATTERN_SEGMENT_NAME_PREFIX.length());
            }
            return EMPTY_STRING;
        }
    }

    private static class PatternSegmentContentAspect implements ISegmentAspect {
        public static final @NonNull ISegmentAspect INSTANCE = new PatternSegmentContentAspect();

        private PatternSegmentContentAspect() {}

        @Override
        public String getHelpText() {
            return checkNotNull("Content");
        }
        @Override
        public String getName() {
            return checkNotNull("Content");
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof TmfXmlPatternSegment) {
                List<String> values = ((TmfXmlPatternSegment) segment).getContent().entrySet().stream().map(c -> c.getKey() + '=' + c.getValue()).collect(Collectors.toList());
                return String.join(", ", values); //$NON-NLS-1$
            } else if (segment instanceof TmfXmlPatternCompositeSegment) {
                List<String> values = ((TmfXmlPatternCompositeSegment) segment).getContent().entrySet().stream().map(c -> c.getKey() + '=' + c.getValue()).collect(Collectors.toList());
                return String.join(", ", values); //$NON-NLS-1$
            }
            return EMPTY_STRING;
        }
    }

    private static List<ISegmentAspect> BASED_TABLE_ASPECTS = ImmutableList.of(StartAspect.INSTANCE, EndAspect.INSTANCE,
            DurationAspect.INSTANCE);
    private static final class StartAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new StartAspect();

        private StartAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull(Messages.SegmentStoreTableViewer_startTime);
        }

        @Override
        public String getName() {
            return checkNotNull(Messages.SegmentStoreTableViewer_startTime);
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return TmfTimestampFormat.getDefaulTimeFormat().format(segment.getStart());
        }
    }

    private static final class EndAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new EndAspect();

        private EndAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull(Messages.SegmentStoreTableViewer_endTime);
        }

        @Override
        public String getName() {
            return checkNotNull(Messages.SegmentStoreTableViewer_endTime);
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return TmfTimestampFormat.getDefaulTimeFormat().format(segment.getEnd());
        }
    }

    private static final class DurationAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new DurationAspect();

        private DurationAspect() {
        }

        @Override
        public String getHelpText() {
            return checkNotNull(Messages.SegmentStoreTableViewer_duration);
        }

        @Override
        public String getName() {
            return checkNotNull(Messages.SegmentStoreTableViewer_duration);
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            return DECIMAL_FORMAT.format(segment.getLength());
        }
    }
}
