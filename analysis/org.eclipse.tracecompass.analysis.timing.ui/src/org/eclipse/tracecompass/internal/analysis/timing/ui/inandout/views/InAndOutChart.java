package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.Iterables;

public class InAndOutChart extends AbstractTimeGraphView {

    private static final RGBA CONTENTION_COLOR = new RGBA(200, 20, 20, 150);
    public static final String ID = "org.eclipse.tracecompass.inandoutchart";
    private InAndOutAnalysis fAnalyis;

    public InAndOutChart() {
        super(ID, new TimeGraphPresentationProvider() {
            private final StateItem[] SI = { new StateItem(new RGB(124, 201, 223)) };

            @Override
            public StateItem[] getStateTable() {
                return SI;
            }
        });
    }

    private static final class InAndOutGraphEntry extends TimeGraphEntry {
        private ISegmentStore fSs;

        private InAndOutGraphEntry(String name, long startTime, long endTime, ISegmentStore ss) {
            super(name, startTime, endTime);
            fSs = ss;
        }

        private ISegmentStore getSs() {
            return fSs;
        }
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        IAnalysisModule module = trace.getAnalysisModule(InAndOutAnalysis.ID);
        if (!(module instanceof InAndOutAnalysis)) {
            return;
        }
        InAndOutAnalysis inAndOutAnalysis = (InAndOutAnalysis) module;
        fAnalyis = inAndOutAnalysis;
        ISegmentAspect aspect = Iterables.<@Nullable ISegmentAspect> getFirst(inAndOutAnalysis.getSegmentAspects(), null);
        if (aspect == null) {
            return;
        }
        ISegmentStore<@NonNull ISegment> segmentStore = inAndOutAnalysis.getSegmentStore();
        if (segmentStore == null) {
            return;
        }
        List<@Nullable Object> a = segmentStore.stream().map(seg -> aspect.resolve(seg)).distinct().collect(Collectors.toList());
        long startTime = trace.getStartTime().toNanos();
        long endTime = trace.getEndTime().toNanos();
        setStartTime(startTime);
        setEndTime(endTime);
        TimeGraphEntry entry = new TimeGraphEntry(trace.getName(), startTime, endTime);

        for (Object elem : a) {
            InAndOutGraphEntry child = new InAndOutGraphEntry(String.valueOf(elem), startTime, endTime, inAndOutAnalysis.getSegmentStore());
            List<@NonNull ITimeEvent> eventList = getEventList(child, startTime, endTime, 1, monitor);
            if (eventList != null) {
                for (ITimeEvent event : eventList) {
                    child.addEvent(event);

                }
            }
            entry.addChild(child);
        }

        addToEntryList(parentTrace, Collections.singletonList(entry));
        refresh();

    }

    @Override
    protected @Nullable List<@NonNull ITimeEvent> getEventList(@NonNull TimeGraphEntry entry, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        if (!(entry instanceof InAndOutGraphEntry)) {
            return Collections.emptyList();
        }
        List<ITimeEvent> ret = new ArrayList<>();
        InAndOutGraphEntry inAndOutGraphEntry = (InAndOutGraphEntry) entry;
        Iterable<ISegment> list = inAndOutGraphEntry.getSs().getIntersectingElements(startTime, endTime);
        for (ISegment elem : list) {
            if (elem instanceof InAndOutSegment) {
                InAndOutSegment inAndOutSegment = (InAndOutSegment) elem;
                if (((InAndOutSegment) elem).getName().equals(entry.getName())) {
                    ret.add(new TimeEvent(entry, inAndOutSegment.getStart(), inAndOutSegment.getLength()));
                }
            }
        }
        return ret;
    }

    @Override
    protected @NonNull List<String> getMarkerCategories() {
        InAndOutAnalysis module = fAnalyis;
        if (module == null) {
            return super.getMarkerCategories();
        }
        List<String> ret = new ArrayList<>();
        ISegmentAspect aspect = Iterables.<@Nullable ISegmentAspect> getFirst(module.getSegmentAspects(), null);
        if (aspect == null) {
            return super.getMarkerCategories();
        }
        ret.addAll(super.getMarkerCategories());
        ret.addAll(module.getMarkers().stream().map(seg -> getMarkerTitle(aspect, seg)).distinct().collect(Collectors.<String> toList()));
        return ret;
    }

    @Override
    protected @NonNull List<IMarkerEvent> getViewMarkerList(long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        InAndOutAnalysis module = fAnalyis;
        List<IMarkerEvent> ret = new ArrayList<>();
        ret.addAll(super.getViewMarkerList(startTime, endTime, resolution, monitor));
        if (module == null) {
            return ret;
        }
        ISegmentAspect aspect = Iterables.<@Nullable ISegmentAspect> getFirst(module.getSegmentAspects(), null);
        if (aspect == null) {
            return ret;
        }
        ret.addAll(module.getMarkers().stream().map(segment -> new MarkerEvent(null, segment.getStart(), segment.getLength(), getMarkerTitle(aspect, segment), CONTENTION_COLOR, getMarkerTitle(aspect, segment), false)).collect(Collectors.toList()));
        return ret;
    }

    private static @NonNull String getMarkerTitle(ISegmentAspect aspect, @NonNull ISegment seg) {
        return "Potential block by " + String.valueOf(aspect.resolve(seg)); //$NON-NLS-1$
    }

}
