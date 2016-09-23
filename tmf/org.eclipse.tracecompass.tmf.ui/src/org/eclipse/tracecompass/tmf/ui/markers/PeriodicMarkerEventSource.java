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

package org.eclipse.tracecompass.tmf.ui.markers;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

/**
 * Marker event source that produces periodic markers.
 *
 * @since 2.0
 */
@NonNullByDefault
public class PeriodicMarkerEventSource implements IMarkerEventSource {

    /**
     * Reference marker time and index
     */
    public static class Reference {

        /** Reference marker index 0 at time 0 */
        public static final Reference ZERO = new Reference(0L, 0);

        private final long time;
        private final long index;

        /**
         * Constructor
         *
         * @param time
         *            the reference marker time in time units
         * @param index
         *            the reference marker index
         */
        public Reference(long time, int index) {
            this.time = time;
            this.index = index;
        }

        /**
         * Constructor
         *
         * @param time
         *            the reference marker time in time units
         * @param index
         *            the reference marker index
         * @since 2.2
         */
        public Reference(long time, long index) {
            this.time = time;
            this.index = index;
        }
    }

    private final String fCategory;
    private final Reference fReference;
    private final double fPeriod;
    private final long fRollover;
    private final @Nullable RGBA[] fColors;
    private final int[] fWeights;
    private final long fTotalWeight;
    private final boolean fForeground;

    /**
     * Constructs a periodic marker event source with line markers at period
     * boundaries.
     * <p>
     * The markers will have the given category and color. The reference defines
     * the marker with the given index to be at the specified time.
     *
     * @param category
     *            the marker category
     * @param reference
     *            the reference marker time and index
     * @param period
     *            the period in time units
     * @param rollover
     *            the number of periods before the index rolls-over to 0, or 0
     *            for no roll-over
     * @param color
     *            the marker color
     * @param foreground
     *            true if the marker is drawn in foreground, and false otherwise
     */
    public PeriodicMarkerEventSource(String category, Reference reference, double period, long rollover, RGBA color, boolean foreground) {
        this(category, reference, period, rollover, foreground, new int[0], color);
    }

    /**
     * Constructs a periodic marker event source with alternating shading
     * markers.
     * <p>
     * The markers will have the given category. Periods with even index will be
     * shaded with the even color. Periods with odd index will be shaded with
     * the odd color. The reference defines the marker with the given index to
     * be at the specified time.
     *
     * @param category
     *            the marker category
     * @param reference
     *            the reference marker time and index
     * @param period
     *            the period in time units
     * @param rollover
     *            the number of periods before the index rolls-over to 0, or 0
     *            for no roll-over
     * @param evenColor
     *            the even marker color
     * @param oddColor
     *            the odd marker color
     * @param foreground
     *            true if the marker is drawn in foreground, and false otherwise
     */
    public PeriodicMarkerEventSource(String category, Reference reference, double period, long rollover, RGBA evenColor, RGBA oddColor, boolean foreground) {
        this(category, reference, period, rollover, foreground, new int[0], evenColor, oddColor);
    }

    /**
     * Constructs a periodic marker event source which splits each period into
     * markers of different weighted lengths.
     * <p>
     * The markers will have the given category. Each element in the weight
     * array will create a marker with a duration equal to the period multiplied
     * by the proportion of the weight over the total weight of all elements in
     * the array. The marker will have the color of its corresponding element in
     * the color array. A null color will not create any corresponding marker.
     * The reference defines the period with the given index to be at the
     * specified time.
     *
     * @param category
     *            the marker category
     * @param reference
     *            the reference marker time and index
     * @param period
     *            the period in time units
     * @param rollover
     *            the number of periods before the index rolls-over to 0, or 0
     *            for no roll-over
     * @param weights
     *            the marker weight array
     * @param colors
     *            the marker color array
     * @param foreground
     *            true if the marker is drawn in foreground, and false otherwise
     * @since 2.2
     */
    public PeriodicMarkerEventSource(String category, Reference reference, double period, long rollover, int[] weights, @Nullable RGBA[] colors, boolean foreground) {
        this(category, reference, period, rollover, foreground, weights, colors);
        if (weights.length != colors.length) {
            throw new IllegalArgumentException("weight and color arrays cannot have different lengths"); //$NON-NLS-1$
        }
        if (weights.length == 0) {
            throw new IllegalArgumentException("weight and color arrays cannot have zero length"); //$NON-NLS-1$
        }
    }

    /* Private constructor. */
    private PeriodicMarkerEventSource(String category, Reference reference, double period, long rollover, boolean foreground, int[] weights, @Nullable RGBA... colors) {
        if (period <= 0) {
            throw new IllegalArgumentException("period cannot be less than or equal to zero"); //$NON-NLS-1$
        }
        if (rollover < 0) {
            throw new IllegalArgumentException("rollover cannot be less than zero"); //$NON-NLS-1$
        }
        fCategory = category;
        fReference = reference;
        fPeriod = period;
        fRollover = rollover;
        fWeights = weights;
        fTotalWeight = Arrays.stream(weights).sum();
        fColors = colors;
        fForeground = foreground;
    }

    @Override
    public List<String> getMarkerCategories() {
        return Arrays.asList(fCategory);
    }

    @Override
    public List<IMarkerEvent> getMarkerList(String category, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        if (startTime > endTime) {
            return Collections.emptyList();
        }
        List<IMarkerEvent> markers = new ArrayList<>();
        long time = startTime;
        if (Math.round((Math.round((time - fReference.time) / fPeriod)) * fPeriod) + fReference.time >= time) {
            /* Subtract one period to ensure previous marker is included */
            time -= Math.max(Math.round(fPeriod), resolution);
        }
        while (true) {
            long index = Math.round((time - fReference.time) / fPeriod) + fReference.index;
            time = Math.round((index - fReference.index) * fPeriod) + fReference.time;
            long labelIndex = fWeights.length == 0 ? index : index * fWeights.length;
            if (fRollover != 0) {
                labelIndex = (labelIndex % fRollover + fRollover) % fRollover;
            }
            if (isApplicable(time)) {
                if (fWeights.length != 0) {
                    int weight = 0;
                    long start = time;
                    for (int i = 0; i < fWeights.length; i++) {
                        if (start < startTime) {
                            /* Only the last previous marker out of range is included */
                            markers.clear();
                        }
                        weight += fWeights[i];
                        long end = time + Math.round((weight / (double) fTotalWeight) * fPeriod);
                        RGBA color = fColors[i];
                        if (color != null) {
                            markers.add(new MarkerEvent(null, start, end - start, fCategory, color, getMarkerLabel(labelIndex), fForeground));
                        }
                        labelIndex++;
                        if (fRollover != 0) {
                            labelIndex = labelIndex % fRollover;
                        }
                        if (start > endTime) {
                            /* Only the first next marker out of range is included */
                            return markers;
                        }
                        start = end;
                    }
                } else if (fColors.length == 1) {
                    markers.add(new MarkerEvent(null, time, 0, fCategory, fColors[0], getMarkerLabel(labelIndex), fForeground));
                } else {
                    RGBA color = fColors[(int) (index & 1)];
                    long duration = Math.round((index + 1 - fReference.index) * fPeriod) + fReference.time - time;
                    markers.add(new MarkerEvent(null, time, duration, fCategory, color, getMarkerLabel(labelIndex), fForeground));
                }
            }
            if (time > endTime) {
                /* The next marker out of range is included */
                break;
            }
            time += Math.max(Math.round(fPeriod), resolution);
        }
        return markers;
    }

    /**
     * Get the marker label for the given marker index.
     * <p>
     * This method can be overridden by clients.
     *
     * @param index
     *            the marker index
     * @return the marker label
     */
    public String getMarkerLabel(long index) {
        return checkNotNull(Long.toString(index));
    }

    /**
     * Get the marker index for the given marker time.
     *
     * @param time
     *            the marker time
     * @return the marker index
     * @since 2.2
     */
    public long getMarkerIndex(long time) {
        long index = Math.round((time - fReference.time) / fPeriod) + fReference.index;
        if (Math.round((index - fReference.index) * fPeriod) + fReference.time > time) {
            index--;
        }
        if (fWeights.length != 0) {
            long firstTime = Math.round((index - fReference.index) * fPeriod) + fReference.time;
            index *= fWeights.length;
            if (fRollover != 0) {
                index = (index % fRollover + fRollover) % fRollover;
            }
            int weight = 0;
            int i = 0;
            while (i < fWeights.length) {
                weight += fWeights[i];
                long end = firstTime + Math.round((weight / (double) fTotalWeight) * fPeriod);
                if (time < end) {
                    break;
                }
                i++;
            }
            return index + i;
        } else if (fRollover != 0) {
            index = (index % fRollover + fRollover) % fRollover;
        }
        return index;
    }

    /**
     * Returns true if the marker is applicable at the specified time.
     * <p>
     * This method can be overridden by clients. Returning false will
     * essentially filter-out the marker.
     *
     * @param time
     *            the marker time
     * @return true if the marker is applicable
     * @since 2.2
     */
    public boolean isApplicable(long time) {
        return true;
    }
}
