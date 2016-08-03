/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.signal;

import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Signal indicating an event filter has been applied.
 *
 * @author Patrick Tasse
 * @since 1.1
 */
public class TmfSegmentFilterAppliedSignal extends TmfSignal {

    private final ITmfTrace fTrace;
    private final ISegmentFilter fFilter;

    /**
     * Constructor for a new signal.
     *
     * @param trace
     *            The trace to which filter is applied
     * @param source
     *            The source of the signal
     * @param segmentFilter
     *            The filter to apply
     */
    public TmfSegmentFilterAppliedSignal(ITmfTrace trace, Object source, ISegmentFilter segmentFilter) {
        super(source);
        fTrace = trace;
        fFilter = segmentFilter;
    }

    /**
     * Get the trace object concerning this signal
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the applied segment filter
     *
     * @return The filter
     */
    public ISegmentFilter getFilter() {
        return fFilter;
    }

    @Override
    public String toString() {
        return "[TmfSegmentFilterAppliedSignal (" + fTrace.getName() + " : " + fFilter + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
