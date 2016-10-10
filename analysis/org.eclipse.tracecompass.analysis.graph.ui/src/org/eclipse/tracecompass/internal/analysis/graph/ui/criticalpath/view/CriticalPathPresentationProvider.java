/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.ui.criticalpath.view;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.ui.util.TmfColorRegistry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Presentation provider for the critical path view
 *
 * @author Geneviève Bastien
 */
public class CriticalPathPresentationProvider extends TimeGraphPresentationProvider {

    /**
     * The enumeration of possible states for the view
     */
    public static enum State {
        /** Worker is running */
        RUNNING,
        /** Worker is interrupted */
        INTERRUPTED,
        /** Worker has been preempted */
        PREEMPTED,
        /** Worker waiting on a timer */
        TIMER,
        /** Worker is blocked, waiting on a device */
        BLOCK_DEVICE,
        /** Worker is waiting for user input */
        USER_INPUT,
        /** Worker is waiting on network */
        NETWORK,
        /** Worker is waiting for an IPI */
        IPI,
        /** Any other reason */
        UNKNOWN;

        /** RGB color associated with a state */
        public final RGB rgb;

        private State() {
            this.rgb = TmfColorRegistry.getInstance().getColor(toString());
        }
    }

    @Override
    public String getStateTypeName() {
        return Messages.getMessage(Messages.CriticalFlowView_stateTypeName);
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            return ((TimeEvent) event).getValue();
        }
        return TRANSPARENT;
    }

    private static State getMatchingState(int status) {
        switch (status) {
        case 0:
            return State.RUNNING;
        case 1:
            return State.INTERRUPTED;
        case 2:
            return State.PREEMPTED;
        case 3:
            return State.TIMER;
        case 4:
            return State.BLOCK_DEVICE;
        case 5:
            return State.USER_INPUT;
        case 6:
            return State.NETWORK;
        case 7:
            return State.IPI;
        default:
            return State.UNKNOWN;
        }
    }

    @Override
    public String getEventName(@Nullable ITimeEvent event) {
        if (event instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent) event;
            if (ev.hasValue()) {
                return NonNullUtils.nullToEmptyString(getMatchingState(ev.getValue()));
            }
        }
        return Messages.getMessage(Messages.CriticalFlowView_multipleStates);
    }

    @Override
    @NonNullByDefault({})
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> eventHoverToolTipInfo = super.getEventHoverToolTipInfo(event, hoverTime);
        if (eventHoverToolTipInfo == null) {
            eventHoverToolTipInfo = new LinkedHashMap<>();
        }
        ITimeGraphEntry entry = event.getEntry();
        if (entry instanceof CriticalPathEntry) {
            CriticalPathEntry criticalPathEntry = (CriticalPathEntry) entry;
            Map<String, String> info = criticalPathEntry.getWorker().getWorkerInformation(hoverTime);
            eventHoverToolTipInfo.putAll(info);
        }
        return eventHoverToolTipInfo;
    }
}
