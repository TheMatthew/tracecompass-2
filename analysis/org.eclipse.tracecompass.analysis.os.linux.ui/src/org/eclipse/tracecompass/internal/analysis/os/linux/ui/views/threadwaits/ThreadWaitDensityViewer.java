/******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.threadwaits;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadwaits.ThreadWaitAnalysis;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Thread Wait density viewer
 *
 * @author Matthew Khouzam
 */
public class ThreadWaitDensityViewer extends AbstractSegmentStoreDensityViewer {

    /**
     * Constructs a new density viewer.
     *
     * @param parent
     *            the parent of the viewer
     */
    public ThreadWaitDensityViewer(Composite parent) {
        super(parent);
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreAnalysisModule(ITmfTrace trace) {
        return TmfTraceUtils.getAnalysisModuleOfClass(trace, ThreadWaitAnalysis.class, ThreadWaitAnalysis.ID);
    }
}