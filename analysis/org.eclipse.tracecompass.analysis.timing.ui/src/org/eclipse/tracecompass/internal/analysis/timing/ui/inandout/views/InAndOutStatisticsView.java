package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class InAndOutStatisticsView extends AbstractSegmentStoreStatisticsView {

    /** The analysis module ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.inandout.statistics.view"; //$NON-NLS-1$

    @Override
    protected @NonNull AbstractSegmentStoreStatisticsViewer createSegmentStoreStatisticsViewer(@NonNull Composite parent) {
        return new AbstractSegmentStoreStatisticsViewer(parent) {

            @Override
            protected @Nullable TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
                return new AbstractSegmentStatisticsAnalysis() {

                    @Override
                    protected @Nullable String getSegmentType(@NonNull ISegment segment) {
                        if (segment instanceof InAndOutSegment) {
                            return ((InAndOutSegment) segment).getName();
                        }
                        return null;
                    }

                    @Override
                    protected @Nullable ISegmentStoreProvider getSegmentProviderAnalysis(@NonNull ITmfTrace trace) {
                        return (ISegmentStoreProvider) trace.getAnalysisModule(InAndOutAnalysis.ID);
                    }

                };
            }

        };
    }

}