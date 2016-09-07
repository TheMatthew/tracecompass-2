/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Messages;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority.PriorityEntry.Type;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.util.Pair;
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
                    if (timeEvent.getValue() == StateValues.CPU_STATUS_RUN_USERMODE) {
                        return 0;
                    }
                    if (timeEvent.getValue() == StateValues.CPU_STATUS_RUN_SYSCALL) {
                        return 1;
                    }
                    return INVISIBLE;
                }
            }

            return 1;
        }
        return INVISIBLE;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> retMap = new LinkedHashMap<>();
        if (!(event instanceof TimeEvent) || !((TimeEvent) event).hasValue() ||
                !(event.getEntry() instanceof PriorityEntry)) {
            return retMap;
        }



        PriorityEntry entry = (PriorityEntry) event.getEntry();
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(entry.getTrace(), KernelAnalysisModule.ID);
        if (ssq == null) {
            return retMap;
        }

        int threadQuark = entry.getQuark();
        if (threadQuark != -1) {
            try {
                @NonNull
                Pair<@NonNull Integer, @NonNull Integer> threadAndCPU = Attributes.parseThreadAttributeName(ssq.getAttributeName(threadQuark));
                int tid = threadAndCPU.getFirst();
                // Find every CPU first, then get the current thread
                int cpusQuark = ssq.getQuarkAbsolute(Attributes.CPUS);
                List<Integer> cpuQuarks = ssq.getSubAttributes(cpusQuark, false);
                for (Integer cpuQuark : cpuQuarks) {
                    int currentThreadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
                    ITmfStateInterval interval = ssq.querySingleState(event.getTime(), currentThreadQuark);
                    if (!interval.getStateValue().isNull()) {
                        ITmfStateValue state = interval.getStateValue();
                        int currentThreadId = state.unboxInt();
                        if (tid == currentThreadId) {
                            retMap.put(Messages.ControlFlowView_attributeCpuName, ssq.getAttributeName(cpuQuark));
                            break;
                        }
                    }
                }
            } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
                Activator.getDefault().logError("Error in ControlFlowPresentationProvider", e); //$NON-NLS-1$
            } catch (StateSystemDisposedException e) {
                /* Ignored */
            }
        }

        return retMap;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[2];
        stateTable[0] = new StateItem(new RGB(100, 200, 100));
        stateTable[1] = new StateItem(new RGB(100, 100, 200));
        return stateTable;
    }
}
