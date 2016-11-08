package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.Activator;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumFields.Phase;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
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
    protected @Nullable String getProcessName(@NonNull ITmfEvent event) {
        return event.getContent().getFieldValue(String.class, "pid");
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
        return (event instanceof ChromiumEvent);
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        if (event instanceof ChromiumEvent && Phase.Begin.equals(((ChromiumEvent)event).getPhase())) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        if (event instanceof ChromiumEvent && Phase.End.equals(((ChromiumEvent)event).getPhase())) {
            return TmfStateValue.newValueString(event.getName());
        }
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
        ChromiumEvent chrEvent = (ChromiumEvent) event;
        String processName = getProcessName(event);
        Phase ph = chrEvent.getPhase();
        if(ph==null) {
            return;
        }
        switch(ph){
        case Begin:
            ITmfStateValue functionBeginName = functionEntry(event);
            if (functionBeginName != null) {
                startHandle(event, ss, timestamp, processName, functionBeginName);
                return;
            }
            break;

        case Complete:
            Long duration = event.getContent().getFieldValue(Long.class, "dur");
            if (duration == null || duration < 0L) {
                return;
            }
            long end = timestamp + duration;
            if (processName == null) {
                int processId = getProcessId(event);
                processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId).intern();
            }
            if (processName.equals("GPU")) {
                new Object();
            }
            int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);

            String threadName = getThreadName(event);
            long threadId = getThreadId(event);
            if (threadName == null) {
                threadName = Long.toString(threadId).intern();
            }
            int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);

            int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
            ITmfStateValue value = TmfStateValue.newValueString(event.getName());

            List<Integer> subs = ss.getSubAttributes(callStackQuark, false);
            try {
                if (end < ss.getCurrentEndTime()) {
                    for (int sub : subs) {
                        ITmfStateInterval stateValue = ss.querySingleState(timestamp, sub);
                        if (stateValue.getStateValue().isNull()) {
                            writeSegmentToSs(ss, timestamp, end, value, sub);
                            return;
                        }
                    }
                    int quark = ss.getQuarkRelativeAndAdd(callStackQuark, Integer.toString(subs.size() + 1));
                    writeSegmentToSs(ss, timestamp, end, value, quark);
                    return;
                }
                int quark = ss.getQuarkRelativeAndAdd(callStackQuark, "1"); //$NON-NLS-1$
                writeSegmentToSs(ss, timestamp, end, value, quark);

            } catch (StateSystemDisposedException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
            break;

        case End:
            /* Check if the event is a function exit */
            ITmfStateValue functionExitState = functionExit(event);
            if (functionExitState != null) {
                endHandle(event, ss, timestamp, processName);
            }
            break;

        case Start:
            ITmfStateValue functionStartName = functionEntry(event);
            if (functionStartName != null) {
                startHandle(event, ss, timestamp, processName, functionStartName);
                return;
            }
            break;
            //$CASES-OMITTED$
        default:
            return;
        }
    }

    private void startHandle(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName, ITmfStateValue functionEntryName) {
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);

        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId);
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);

        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
        ITmfStateValue value = functionEntryName;
        ss.pushAttribute(timestamp, value, callStackQuark);
    }

    private void endHandle(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        String pName = processName;

        if (pName == null) {
            int processId = getProcessId(event);
            pName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        }
        String threadName = getThreadName(event);
        if (threadName == null) {
            threadName = Long.toString(getThreadId(event));
        }
        int quark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName, threadName, CALL_STACK);
        ss.popAttribute(timestamp, quark);
    }

    protected void writeSegmentToSs(@NonNull ITmfStateSystemBuilder ss, long timestamp, long end, @NonNull ITmfStateValue value, int quark) {
        ss.modifyAttribute(timestamp, value, quark);
        ss.modifyAttribute(end, TmfStateValue.nullValue(), quark);
    }
}
