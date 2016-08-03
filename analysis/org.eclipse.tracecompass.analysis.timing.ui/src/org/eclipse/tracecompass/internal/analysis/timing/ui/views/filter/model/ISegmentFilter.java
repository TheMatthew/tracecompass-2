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

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * The segment store filter interface.
 *
 * @author Jean-Christian Kouame
 * @since 1.1
 */
public interface ISegmentFilter {

    /**
     * String value representing the filter id prefix in the trace context data
     */
    static final String FILTER_PREFIX = "ISEGMENTFILTER"; //$NON-NLS-1$

    /**
     * Verify the filter conditions on an event
     *
     * @param segment
     *            The segment to verify.
     * @return True if the event matches the filter conditions.
     */
    boolean matches(ISegment segment);

    /**
     * Verify that the filter is active
     *
     * @return the active state
     */
    boolean isActive();

    /**
     * Set the active state of the filter
     *
     * @param isActive
     *            The new state
     */
    void setActive(boolean isActive);

}
