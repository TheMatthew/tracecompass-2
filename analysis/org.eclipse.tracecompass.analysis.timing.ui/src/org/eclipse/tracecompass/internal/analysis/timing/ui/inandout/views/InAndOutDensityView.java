package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class InAndOutDensityView extends AbstractSegmentStoreDensityView {

    /** The analysis module ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.innout.density"; //$NON-NLS-1$

    public InAndOutDensityView() {
        super(ID);
    }

    @Override
    protected @NonNull AbstractSegmentStoreTableViewer createSegmentStoreTableViewer(@NonNull Composite parent) {
        // TODO Auto-generated method stub
        return new AbstractSegmentStoreTableViewer(new TableViewer(parent, SWT.FULL_SELECTION | SWT.VIRTUAL)) {
            @Override
            protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
                return (@Nullable ISegmentStoreProvider) trace.getAnalysisModule(InAndOutAnalysis.ID);
            }
        };
    }

    @Override
    protected @NonNull AbstractSegmentStoreDensityViewer createSegmentStoreDensityViewer(@NonNull Composite parent) {
        return new AbstractSegmentStoreDensityViewer(parent) {
            @Override
            protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
                return (@Nullable ISegmentStoreProvider) trace.getAnalysisModule(InAndOutAnalysis.ID);
            }
        };
    }

}