package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class InAndOutTableView extends AbstractSegmentStoreTableView {


    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.inandout.views.latency"; //$NON-NLS-1$


    @Override
    protected @NonNull AbstractSegmentStoreTableViewer createSegmentStoreViewer(@NonNull TableViewer tableViewer) {
        return new AbstractSegmentStoreTableViewer(tableViewer) {
            @Override
            protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
                return (@Nullable ISegmentStoreProvider) trace.getAnalysisModule(InAndOutAnalysis.ID);
            }
        };
    }

}
