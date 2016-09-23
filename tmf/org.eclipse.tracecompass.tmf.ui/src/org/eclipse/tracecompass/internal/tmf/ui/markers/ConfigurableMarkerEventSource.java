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

package org.eclipse.tracecompass.internal.tmf.ui.markers;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.internal.tmf.ui.markers.Marker.SplitMarker;
import org.eclipse.tracecompass.internal.tmf.ui.markers.Marker.WeightedMarker;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.markers.PeriodicMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.markers.PeriodicMarkerEventSource.Reference;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;

import com.google.common.collect.Lists;
import com.google.common.collect.RangeSet;

/**
 * Configurable marker event source.
 */
public class ConfigurableMarkerEventSource implements IMarkerEventSource {

    private static final long NANO_PER_MILLI = 1000000L;
    private static final long NANO_PER_MICRO = 1000L;
    private static final int MIN_PERIOD = 5; // in units of resolution intervals

    private ITmfTrace fTrace;
    private Reference fReference;
    private Map<IMarkerEventSource, Double> fMarkerEventSources;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public ConfigurableMarkerEventSource(ITmfTrace trace) {
        fTrace = trace;
        fMarkerEventSources = new LinkedHashMap<>();
    }

    /**
     * Configure the marker source from the specified marker set
     *
     * @param markerSet
     *            the marker set, or null to clear the configuration
     */
    public synchronized void configure(MarkerSet markerSet) {
        fMarkerEventSources.clear();
        if (markerSet != null) {
            for (Marker marker : markerSet.getMarkers()) {
                fReference = null;
                configure(marker, null);
            }
        }
    }

    private void configure(Marker marker, MarkerEventSource parent) {
        if (marker instanceof SplitMarker) {
            SplitMarker splitMarker = (SplitMarker) marker;
            long rollover = splitMarker.getRange().hasUpperBound() ? (splitMarker.getRange().upperEndpoint() - splitMarker.getRange().lowerEndpoint() + 1) : 0;
            @NonNull RGBA evenColor = checkNotNull(splitMarker.getColor());
            RGBA oddColor = new RGBA(evenColor.rgb.red, evenColor.rgb.green, evenColor.rgb.blue, 0);
            double period = convertToNanos(splitMarker.getPeriod(), splitMarker.getUnit());
            if (fReference == null) {
                long offset = Math.round(convertToNanos(splitMarker.getOffset(), splitMarker.getUnit()));
                if (rollover != 0) {
                    long startTime = fTrace.getStartTime().toNanos();
                    long rolloverTime = Math.round(period * rollover);
                    fReference = new Reference(startTime - (startTime - offset) % rolloverTime, 0);
                } else {
                    fReference = new Reference(offset, 0);
                }
            }
            MarkerEventSource markerEventSource = new MarkerEventSource(checkNotNull(splitMarker.getName()), checkNotNull(fReference), period, rollover, evenColor, oddColor, false, parent, splitMarker.getRange().lowerEndpoint(), checkNotNull(splitMarker.getLabel()), splitMarker.getIndexRange());
            fMarkerEventSources.put(markerEventSource, period);
            for (Marker subMarker : splitMarker.getSubMarkers()) {
                configure(subMarker, markerEventSource);
            }
        } else if (marker instanceof WeightedMarker) {
            WeightedMarker weightedMarker = (WeightedMarker) marker;
            List<MarkerSegment> segments = weightedMarker.getSegments();
            long rollover = segments.size();
            int[] weights = new int[segments.size()];
            @Nullable RGBA @NonNull[] colors = new @Nullable RGBA[segments.size()];
            @NonNull String[] labels = new @NonNull String[segments.size()];
            double period = convertToNanos(weightedMarker.getPeriod(), weightedMarker.getUnit());
            double maxSegmentPeriod = 0;
            long totalLength = weightedMarker.getTotalLength();
            for (int i = 0; i < segments.size(); i++) {
                MarkerSegment segment = segments.get(i);
                weights[i] = segment.getLength();
                colors[i] = segment.getColor();
                labels[i] = checkNotNull(segment.getLabel());
                maxSegmentPeriod = Math.max(maxSegmentPeriod, period * segment.getLength() / totalLength);
            }
            MarkerEventSource markerEventSource = new MarkerEventSource(checkNotNull(weightedMarker.getName()), checkNotNull(fReference), period, rollover, weights, colors, false, parent, labels);
            fMarkerEventSources.put(markerEventSource, maxSegmentPeriod);
        }
    }

    private static double convertToNanos(double number, String unit) {
        if (unit.equalsIgnoreCase(IMarkerConstants.MS)) {
            return number * NANO_PER_MILLI;
        } else if (unit.equalsIgnoreCase(IMarkerConstants.US)) {
            return number * NANO_PER_MICRO;
        } else if (unit.equalsIgnoreCase(IMarkerConstants.NS)) {
            return number;
//        } else if (unit.equalsIgnoreCase(IMarkerConstants.CYCLES)) {
//            if (fTrace instanceof ICyclesConverter) {
//                return ((ICyclesConverter) fTrace).cyclesToNanos((long) number);
//            }
        }
        return number;
    }

    @Override
    public List<String> getMarkerCategories() {
        Set<String> categories = new LinkedHashSet<>();
        for (IMarkerEventSource source : fMarkerEventSources.keySet()) {
            categories.addAll(source.getMarkerCategories());
        }
        return checkNotNull(Lists.newArrayList(categories));
    }

    @Override
    public List<IMarkerEvent> getMarkerList(String category, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        @NonNull List<@NonNull IMarkerEvent> markerList = Collections.EMPTY_LIST;
        for (Entry<IMarkerEventSource, Double> entry : fMarkerEventSources.entrySet()) {
            IMarkerEventSource source = entry.getKey();
            double period = entry.getValue();
            if (source.getMarkerCategories().contains(category) && period > resolution * MIN_PERIOD) {
                @NonNull List<@NonNull IMarkerEvent> list = source.getMarkerList(category, startTime, endTime, resolution, monitor);
                if (markerList.isEmpty()) {
                    markerList = list;
                } else {
                    markerList.addAll(list);
                    markerList.sort(Comparator.comparingLong(marker -> marker.getTime()));
                }
            }
        }
        return markerList;
    }

    private class MarkerEventSource extends PeriodicMarkerEventSource {

        private final MarkerEventSource fParent;
        private final long fStartIndex;
        private final String[] fLabels;
        private final RangeSet<Long> fIndexRange;

        public MarkerEventSource(@NonNull String category, @NonNull Reference reference, double period, long rollover, @NonNull RGBA evenColor, @NonNull RGBA oddColor, boolean foreground, MarkerEventSource parent, long startIndex, @NonNull String label, RangeSet<Long> indexRange) {
            super(category, reference, period, rollover, evenColor, oddColor, foreground);
            fParent = parent;
            fStartIndex = startIndex;
            fLabels = new String[] { label };
            fIndexRange = indexRange;
        }

        public MarkerEventSource(@NonNull String category, @NonNull Reference reference, double period, long rollover, int @NonNull[] weights, @Nullable RGBA @NonNull[] colors, boolean foreground, MarkerEventSource parent, @NonNull String[] labels) {
            super(category, reference, period, rollover, weights, colors, foreground);
            fParent = parent;
            fStartIndex = 0;
            fLabels = labels;
            fIndexRange = null;
        }

        @Override
        public @NonNull String getMarkerLabel(long index) {
            return checkNotNull(String.format(fLabels[(int) index % fLabels.length], fStartIndex + index));
        }

        @Override
        public boolean isApplicable(long time) {
            if (fParent != null && !fParent.isApplicable(time)) {
                return false;
            }
            if (fIndexRange != null) {
                return fIndexRange.contains(fStartIndex + getMarkerIndex(time));
            }
            return true;
        }
    }
}
