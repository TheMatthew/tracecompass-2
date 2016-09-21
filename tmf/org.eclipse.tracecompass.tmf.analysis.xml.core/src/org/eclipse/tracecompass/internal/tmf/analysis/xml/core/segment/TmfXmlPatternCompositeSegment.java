/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.segment;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ICompositeSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Basic implementation of a pattern composite segment
 *
 * @author Jean-Christian Kouame
 *
 */
public class TmfXmlPatternCompositeSegment extends TmfXmlPatternSegment implements ICompositeSegment {


    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 3556323761465412078L;

    private final @NonNull List<ISegment> fSubSegments;

    /**
     * Constructs an XML pattern segment
     *
     * @param start
     *            Start time of the pattern segment
     * @param end
     *            End time of the pattern segment
     * @param scale
     *            Scale of the pattern segment
     * @param segmentName
     *            Name of the pattern segment
     * @param fields
     *            Fields of the pattern segment
     * @param segments
     *            The subsegments
     */
    public TmfXmlPatternCompositeSegment(long start, long end, int scale, String segmentName, @NonNull Map<@NonNull String, @NonNull ITmfStateValue> fields, @NonNull List<@NonNull ISegment> segments) {
        super(start, end, scale, segmentName, fields);
        fSubSegments = segments;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString())
                .append(", [fTimestampStart=").append(getTimestampStart()) //$NON-NLS-1$
                .append(", fTimestampEnd=").append(getTimestampEnd()) //$NON-NLS-1$
                .append(", duration= ").append(getLength()) //$NON-NLS-1$
                .append(", fName=").append(getName()) //$NON-NLS-1$
                .append(", fContent=").append(getContent()) //$NON-NLS-1$
                .append("]"); //$NON-NLS-1$
        builder.append(", sub-segments["); //$NON-NLS-1$
        for (ISegment segment : fSubSegments) {
            builder.append(segment.toString());
        }
        builder.append("]");  //$NON-NLS-1$
        return builder.toString();
    }

    @Override
    public List<ISegment> getSubSegments() {
        return fSubSegments;
    }
}
