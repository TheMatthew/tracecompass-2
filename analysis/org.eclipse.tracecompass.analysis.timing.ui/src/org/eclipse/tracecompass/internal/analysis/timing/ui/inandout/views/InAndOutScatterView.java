package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.scatter.AbstractSegmentStoreScatterGraphViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfChartView;

public class InAndOutScatterView extends TmfChartView {
    /** The analysis module ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.innout.scatter"; //$NON-NLS-1$

    public InAndOutScatterView() {
        super(ID);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        // TODO Auto-generated method stub
        return new AbstractSegmentStoreScatterGraphViewer(parent, "In-N-Out Scatter", "Time", "Duration") {

            @Override
            protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
                return (@Nullable ISegmentStoreProvider) trace.getAnalysisModule(InAndOutAnalysis.ID);
            }
        };
    }

}
