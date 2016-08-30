/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority.PriorityEntry.Type;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Priority view presentation provider
 *
 * @author Matthew Khouzam
 *
 */
public class PriorityPresentationProvider extends TimeGraphPresentationProvider {

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        ITimeGraphEntry entry = event.getEntry();
        if (entry instanceof PriorityEntry) {
            PriorityEntry priorityEntry = (PriorityEntry) entry;
            if (event instanceof TimeEvent) {
                TimeEvent timeEvent = (TimeEvent) event;
                if (priorityEntry.getType() == Type.CPU) {

                    if (timeEvent.getValue() == 0) {
                        return INVISIBLE;
                    }
                    return 0;
                } else if (priorityEntry.getType() == Type.THREAD) {
                    if (timeEvent.getValue() != 2) {
                        return INVISIBLE;
                    }
                }
            }

            return 1;
        }
        return INVISIBLE;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[2];
        stateTable[0] = new StateItem(new RGB(100, 200, 100));
        stateTable[1] = new StateItem(new RGB(100, 100, 200));
        return stateTable;
    }
}
