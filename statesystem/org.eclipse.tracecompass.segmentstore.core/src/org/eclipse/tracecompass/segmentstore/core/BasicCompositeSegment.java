/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.segmentstore.core;

import java.util.List;

import static org.eclipse.tracecompass.common.core.NonNullUtils.*;

import com.google.common.collect.ImmutableList;

/**
 * Basic implementation of a composite segment
 *
 * @author Bernd Hufmann
 * @since 1.0
 */
public class BasicCompositeSegment extends BasicSegment implements ICompositeSegment {

    private static final long serialVersionUID = -5122817746069095913L;

    private final List<ISegment> fSubSegments;

    /**
     * Create a new segment.
     *
     * The end position should be equal to or greater than the start position.
     *
     * @param start
     *            Start position of the segment
     * @param end
     *            End position of the segment
     * @param segments
     *            List of sub segments
     */
    public BasicCompositeSegment(long start, long end, List<ISegment> segments) {
        super(start, end);
        fSubSegments = checkNotNull(ImmutableList.copyOf(segments));
    }

    @Override
    public List<ISegment> getSubSegments() {
        return fSubSegments;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(", sub-segments[");  //$NON-NLS-1$
        for (ISegment segment : fSubSegments) {
            builder.append(segment.toString());
        }
        builder.append("]");  //$NON-NLS-1$
        return checkNotNull(builder.toString());
    }
}
