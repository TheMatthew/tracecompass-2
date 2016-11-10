package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

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
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class ChromiumCallStackProvider extends CallStackStateProvider {

    private PriorityQueue<ChromiumEvent> fEventBuffer = new PriorityQueue<>(new Comparator<ChromiumEvent>() {
        @Override
        public int compare(ChromiumEvent o1, ChromiumEvent o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    });

    private ITmfTimestamp fSafeTime;

    public ChromiumCallStackProvider(@NonNull ITmfTrace trace) {
        super(trace);
        ITmfStateSystemBuilder stateSystemBuilder = getStateSystemBuilder();
        if (stateSystemBuilder != null) {
            int quark = stateSystemBuilder.getQuarkAbsoluteAndAdd("dummy entry to make gpu entries work");
            stateSystemBuilder.modifyAttribute(0, TmfStateValue.newValueInt(0), quark);
        }
        fSafeTime = trace.getStartTime();
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
        if (event instanceof ChromiumEvent && Phase.Begin.equals(((ChromiumEvent) event).getPhase())) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        if (event instanceof ChromiumEvent && Phase.End.equals(((ChromiumEvent) event).getPhase())) {
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
        if (ph == null) {
            return;
        }
        switch (ph) {
        case Begin:
            ITmfStateValue functionBeginName = functionEntry(event);
            if (functionBeginName != null) {
                startHandle(event, ss, timestamp, processName, functionBeginName);
            }
            break;

        case Complete:
            Long duration = event.getContent().getFieldValue(Long.class, "dur");
            if (duration != null && duration >= 0L) {
                handleComplete(chrEvent, ss, processName);
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
            }
            break;
        // $CASES-OMITTED$
        default:
            return;
        }
    }

    private void handleComplete(ChromiumEvent event, ITmfStateSystemBuilder ss, String processName) {

        ITmfTimestamp timestamp = event.getTimestamp();
        fSafeTime = fSafeTime.compareTo(timestamp) > 0 ? fSafeTime : timestamp;
        fEventBuffer.add(event);
        while (!fEventBuffer.isEmpty() && fEventBuffer.peek().getEndTime().compareTo(fSafeTime) < 0) {
            ChromiumEvent eventToWrite = fEventBuffer.poll();
            if (eventToWrite == null) {
                return;
            }
            String processName_ = processName;
            if (processName_ == null) {
                int processId = getProcessId(eventToWrite);
                processName_ = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId).intern();
            }
            int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName_);
            long startTime = eventToWrite.getTimestamp().toNanos();
            long end = eventToWrite.getEndTime().toNanos();
            String threadName = getThreadName(eventToWrite);
            long threadId = getThreadId(eventToWrite);
            if (threadName == null) {
                threadName = Long.toString(threadId).intern();
            }
            int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);

            int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
            ITmfStateValue value = TmfStateValue.newValueString(eventToWrite.getName());

            List<Integer> subs = ss.getSubAttributes(callStackQuark, false);
            try {
                if (end < ss.getCurrentEndTime()) {
                    List<@NonNull ITmfStateInterval> stateValues = ss.queryFullState(startTime);
                    for (int sub : subs) {
                        if (stateValues.get(sub).getStateValue().isNull()) {
                            writeSegmentToSs(ss, startTime, end, value, sub);
                            return;
                        }
                    }
                    int quark = ss.getQuarkRelativeAndAdd(callStackQuark, Integer.toString(subs.size() + 1));
                    writeSegmentToSs(ss, startTime, end, value, quark);
                    return;
                }
                int quark = ss.getQuarkRelativeAndAdd(callStackQuark, "1"); //$NON-NLS-1$
                writeSegmentToSs(ss, startTime, end, value, quark);

            } catch (StateSystemDisposedException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
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

    protected void writeSegmentToSs(@NonNull ITmfStateSystemBuilder ss, long startTime, long endTime, @NonNull ITmfStateValue value, int quark) {
        ss.modifyAttribute(startTime, value, quark);
        ss.modifyAttribute(endTime, TmfStateValue.nullValue(), quark);
    }
}
