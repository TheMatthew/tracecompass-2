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
package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

/**
 * Base class for filter nodes which use event aspects
 *
 * @author Patrick Tasse
 * @since 1.1
 */
public abstract class TmfFilterAspectNode extends TmfFilterTreeNode {

    /** event aspect attribute name */
    public static final String SEGMENT_ASPECT_ATTR = "eventaspect"; //$NON-NLS-1$
    /** field attribute name */
    public static final String FIELD_ATTR = "field"; //$NON-NLS-1$
    /** special case trace type id for base aspects */
    public static final String BASE_ASPECT_ID = "BASE.ASPECT.ID"; //$NON-NLS-1$

    /** event aspect */
    @Nullable protected ISegmentAspect fEventAspect;

    /**
     * @param parent the parent node
     */
    public TmfFilterAspectNode(ITmfFilterTreeNode parent) {
        super(parent);
    }

    /**
     * @return The event aspect of this filter
     */
    public @Nullable ISegmentAspect getEventAspect() {
        return fEventAspect;
    }

    /**
     * @param aspect
     *            The event aspect to assign to this filter
     */
    public void setEventAspect(@Nullable ISegmentAspect aspect) {
        fEventAspect = aspect;
    }

    /**
     * @param explicit
     *            true if the string representation should explicitly include
     *            the trace type id that can differentiate it from other aspects
     *            with the same name
     *
     * @return The string representation of the event aspect
     */
    public String getAspectLabel(boolean explicit) {
        if (fEventAspect == null) {
            return ""; //$NON-NLS-1$
        }
        @NonNull final ISegmentAspect aspect = NonNullUtils.checkNotNull(fEventAspect);
        StringBuilder sb = new StringBuilder(aspect.getName());
        if (explicit) {
            sb.append('[');
            sb.append(']');
        }
        if (fEventAspect instanceof TmfEventFieldAspect) {
            String field = ((TmfEventFieldAspect) fEventAspect).getFieldPath();
            if (field != null && !field.isEmpty()) {
                if (field.charAt(0) != '/') {
                    sb.append('/');
                }
                sb.append(field);
            }
        }
        return sb.toString();
    }

    @Override
    public @Nullable ITmfFilterTreeNode clone() {
        @NonNull final TmfFilterAspectNode clone = (TmfFilterAspectNode) super.clone();
        clone.setEventAspect(NonNullUtils.checkNotNull(fEventAspect));
        return clone;
    }
}

