package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.Activator;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class ChromiumCallStackProvider extends CallStackStateProvider {

    public ChromiumCallStackProvider(@NonNull ITmfTrace trace) {
        super(trace);
        ITmfStateSystemBuilder stateSystemBuilder = getStateSystemBuilder();
        if (stateSystemBuilder != null) {
            int quark = stateSystemBuilder.getQuarkAbsoluteAndAdd("dummy entry to make gpu entries work");
            stateSystemBuilder.modifyAttribute(0, TmfStateValue.newValueInt(0), quark);
        }
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, "pid");
        return fieldValue == null ? -1 : fieldValue.intValue();
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, "tid");
        return fieldValue == null ? -1 : fieldValue.intValue();

    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new ChromiumCallStackProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        return (event instanceof ChromiumEvent) && ((ChromiumEvent) event).getType().getFieldNames().contains("dur");
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        return null;
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        /* Check if the event is a function entry */
        long timestamp = event.getTimestamp().toNanos();
        Long duration = event.getContent().getFieldValue(Long.class, "dur");
        if (duration == null || duration < 0L) {
            return;
        }
        long end = timestamp + duration;
        String processName = getProcessName(event);
        int processId = getProcessId(event);
        if (processName == null) {
            processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId).intern();
        }
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);
        ss.updateOngoingState(TmfStateValue.newValueInt(processId), processQuark);

        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId).intern();
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);
        ss.updateOngoingState(TmfStateValue.newValueLong(threadId), threadQuark);

        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
        ITmfStateValue value = TmfStateValue.newValueString(event.getName());
        try {
            if (timestamp < ss.getCurrentEndTime()) {
                List<Integer> subs = ss.getSubAttributes(callStackQuark, false);
                for (int sub : subs) {
                    try {
                        ITmfStateInterval stateValue = ss.querySingleState(timestamp, sub);
                        if (stateValue.getStateValue().isNull()) {
                            ss.modifyAttribute(timestamp, value, sub);
                            ss.modifyAttribute(end, TmfStateValue.nullValue(), sub);
                            return;
                        }
                    } catch (TimeRangeException e) {
                        break;
                    }

                }
            }
            ss.pushAttribute(timestamp, value, callStackQuark);
            ss.popAttribute(end, callStackQuark);
        } catch (StateSystemDisposedException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return;
    }
}
