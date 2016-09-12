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

/**
 * Interface for composite segments
 *
 * @author Bernd Hufmann
 * @since 1.0
 */
public interface ICompositeSegment {
    /**
     * Returns a ordered list of sub-segments
     *
     * @return list of sub-segments.
     */
    List<ISegment> getSubSegments();
}
