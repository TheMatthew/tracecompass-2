package org.eclipse.tracecompass.internal.analysis.timing.core.inandout;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

public interface IInAndOutHandler {

    void handle(ITmfEvent event, Map<String, ITmfTimestamp> currentStack, Collection<@NonNull ISegment> store, Collection<@NonNull ISegment> markers);

}