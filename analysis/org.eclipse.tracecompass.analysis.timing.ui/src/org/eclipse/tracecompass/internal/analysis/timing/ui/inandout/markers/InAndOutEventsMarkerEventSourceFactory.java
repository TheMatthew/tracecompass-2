/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.markers;

import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.core.trace.AbstractTmfTraceAdapterFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.ui.markers.PeriodicMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.markers.PeriodicMarkerEventSource.Reference;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;

/**
 * Marker event source factory for lost events.
 */
public class InAndOutEventsMarkerEventSourceFactory extends AbstractTmfTraceAdapterFactory {

    @Override
    protected <T> T getTraceAdapter(ITmfTrace trace, Class<T> adapterType) {
        if (IMarkerEventSource.class.equals(adapterType) && trace instanceof ITmfTraceWithPreDefinedEvents) {
            ITmfTraceWithPreDefinedEvents traceWithPreDefinedEvents = (ITmfTraceWithPreDefinedEvents) trace;
            if (traceWithPreDefinedEvents.getContainedEventTypes().stream().anyMatch(eventName -> eventName.getName().endsWith("_begin") || eventName.getName().endsWith("_end"))) {
                IMarkerEventSource adapter = new PeriodicMarkerEventSource("Period", new Reference(trace.getStartTime().toNanos(), 0), 59314293.0, 0, new RGBA(200, 200, 200, 40), new RGBA(200, 200, 200, 80), false);
                return adapterType.cast(adapter);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] {
                IMarkerEventSource.class
        };
    }
}
