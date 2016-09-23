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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGBA;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

/**
 * Model element for periodic marker.
 */
public class Marker {

    private final String fName;
    private final double fPeriod;
    private final String fUnit;
    private final long fOffset;

    /**
     * Private constructor.
     *
     * @param name
     *            the name
     * @param period
     *            the period
     * @param unit
     *            the unit
     * @param offset
     *            the offset
     */
    private Marker(String name, double period, String unit, long offset) {
        super();
        fName = name;
        fPeriod = period;
        fUnit = unit;
        fOffset = offset;
    }

    /**
     * Subclass for periodic marker evenly split into segments of equal length.
     *
     */
    public static class SplitMarker extends Marker {

        private final String fLabel;
        private final String fId;
        private final RGBA fColor;
        private final Range<Long> fRange;
        private final RangeSet<Long> fIndexRange;
        private final List<Marker> fSubMarkers;

        /**
         * Constructor
         *
         * @param name
         *            the name
         * @param label
         *            the label
         * @param id
         *            the id
         * @param color
         *            the color
         * @param period
         *            the period
         * @param unit
         *            the unit
         * @param range
         *            the range
         * @param offset
         *            the offset
         * @param indexRange
         *            the index range
         */
        public SplitMarker(String name, String label, String id, RGBA color, double period, String unit, Range<Long> range, long offset, RangeSet<Long> indexRange) {
            super(name, period, unit, offset);
            fLabel = label;
            fId = id;
            fColor = color;
            fRange = range;
            fIndexRange = indexRange;
            fSubMarkers = new ArrayList<>();
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return fLabel;
        }

        /**
         * @return the id
         */
        public String getId() {
            return fId;
        }

        /**
         * @return the color
         */
        public RGBA getColor() {
            return fColor;
        }

        /**
         * @return the range
         */
        public Range<Long> getRange() {
            return fRange;
        }

        /**
         * @return the index range
         */
        public RangeSet<Long> getIndexRange() {
            return fIndexRange;
        }

        /**
         * @return the sub-markers
         */
        public List<Marker> getSubMarkers() {
            return fSubMarkers;
        }

        /**
         * Add a sub-marker.
         *
         * @param subMarker the sub-marker
         */
        public void addSubMarker(Marker subMarker) {
            fSubMarkers.add(subMarker);
        }
    }

    /**
     * Subclass for a periodic marker divided into segments of specified weighted lengths.
     *
     */
    public static class WeightedMarker extends Marker {

        private final List<MarkerSegment> fSegments;

        /**
         * Constructor
         *
         * @param name
         *            the name
         * @param period
         *            the period
         * @param unit
         *            the unit
         * @param offset
         *            the offset
         */
        public WeightedMarker(String name, double period, String unit, long offset) {
            super(name, period, unit, offset);
            fSegments = new ArrayList<>();
        }

        /**
         * @return the segments
         */
        public List<MarkerSegment> getSegments() {
            return fSegments;
        }

        /**
         * Add a segment.
         *
         * @param segment
         *            the segment
         */
        public void addSegment(MarkerSegment segment) {
            fSegments.add(segment);
        }

        /**
         * Get the total length of all segments
         *
         * @return the total length
         */
        public long getTotalLength() {
            long total = 0;
            for (MarkerSegment segment : fSegments) {
                total += segment.getLength();
            }
            return total;
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * @return the period
     */
    public double getPeriod() {
        return fPeriod;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return fUnit;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return fOffset;
    }
}
