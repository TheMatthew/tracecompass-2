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

import org.eclipse.swt.graphics.RGBA;

/**
 * Model element for periodic marker segment.
 */
public class MarkerSegment {

    private final String fLabel;
    private final String fId;
    private final RGBA fColor;
    private final int fLength;

    /**
     * Constructor
     *
     * @param label
     *            the label
     * @param id
     *            the id
     * @param color
     *            the color
     * @param length
     *            the length
     */
    public MarkerSegment(String label, String id, RGBA color, int length) {
        super();
        fLabel = label;
        fId = id;
        fColor = color;
        fLength = length;
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
     * @return the length
     */
    public int getLength() {
        return fLength;
    }
}
