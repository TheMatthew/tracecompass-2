package org.eclipse.tracecompass.internal.analysis.timing.core.inandout;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

public abstract class AbstractInAndOutEndHandler implements IInAndOutHandler {

    private static final String CONTENTION = "CONTENTION";

    protected abstract String getKey();

    @Override
    public void handle(ITmfEvent event, Map<String, ITmfTimestamp> currentStack, Collection<@NonNull ISegment> store, Collection<@NonNull ISegment> markers) {
        String key = getKey();
        ITmfTimestamp tBegin = currentStack.remove(key);
        if (tBegin == null) {
            tBegin = event.getTrace().getStartTime();
        }
        if (currentStack.size() == 2 && currentStack.containsKey(CONTENTION)) {
            ITmfTimestamp contentionTs = currentStack.remove(CONTENTION);
            if (contentionTs != null) {
                markers.add(new InAndOutSegment(contentionTs.toNanos(), event.getTimestamp().toNanos(), key));
            }
        }
        ISegment fs2Segment = new InAndOutSegment(tBegin.toNanos(), event.getTimestamp().toNanos(), key);
        store.add(fs2Segment);
    }

}