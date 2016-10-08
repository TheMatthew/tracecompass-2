package org.eclipse.tracecompass.internal.analysis.timing.ui.inandout.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.inandout.InAndOutSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An in and out chart. This chart is used to graph the I2C bus on a rover
 *
 * @author Matthew Khouzam
 */
public class InAndOutChart extends AbstractTimeGraphView {

    private static final RGBA PONTENTION_COLOR = new RGBA(200, 160, 20, 150);
    private static final RGBA CONTENTION_COLOR = new RGBA(200, 20, 20, 150);
    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.inandoutchart"; //$NON-NLS-1$
    private InAndOutAnalysis fAnalyis;

    /**
     * Constructor
     */
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
        private ISegmentStore<? extends ISegment> fSs;

        private InAndOutGraphEntry(String name, long startTime, long endTime, ISegmentStore<? extends ISegment> ss) {
            super(name, startTime, endTime);
            fSs = ss;
        }

        private ISegmentStore<? extends ISegment> getSs() {
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
        ISegmentAspect aspect = getNameAspect(inAndOutAnalysis);
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

    private static @Nullable ISegmentAspect getNameAspect(InAndOutAnalysis inAndOutAnalysis) {
        Optional<@NonNull ISegmentAspect> findAny = StreamSupport.stream(inAndOutAnalysis.getSegmentAspects().spliterator(), false).filter(aspect -> aspect instanceof InAndOutAnalysis.NameAspect).findAny();
        return findAny.isPresent() ? findAny.get() : null;
    }

    @Override
    protected @Nullable List<@NonNull ITimeEvent> getEventList(@NonNull TimeGraphEntry entry, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        if (!(entry instanceof InAndOutGraphEntry)) {
            return Collections.emptyList();
        }
        List<@NonNull ITimeEvent> ret = new ArrayList<>();
        InAndOutGraphEntry inAndOutGraphEntry = (InAndOutGraphEntry) entry;
        Iterable<? extends ISegment> list = inAndOutGraphEntry.getSs().getIntersectingElements(startTime, endTime);
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
        ISegmentAspect aspect = getNameAspect(module);
        if (aspect == null) {
            return super.getMarkerCategories();
        }
        ret.addAll(super.getMarkerCategories());
        ret.addAll(module.getMarkers().stream().map(seg -> getMarkerTitle(aspect, seg)).distinct().collect(Collectors.<String> toList()));
        IAnalysisModule moduleFutex = getFutexModule(getTrace());
        if (!(moduleFutex instanceof ISegmentStoreProvider)) {
            return ret;
        }
        ISegmentStoreProvider storeProvider = (ISegmentStoreProvider) moduleFutex;
        ISegmentStore<@NonNull ISegment> segmentStore = storeProvider.getSegmentStore();
        if (segmentStore != null) {
            ret.add(getMarkerTitle2());
        }
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
        ISegmentAspect aspect = getNameAspect(module);
        if (aspect == null) {
            return ret;
        }
        ret.addAll(module.getMarkers().stream().map(segment -> new MarkerEvent(null, segment.getStart(), segment.getLength(), getMarkerTitle(aspect, segment), PONTENTION_COLOR, getMarkerTitle(aspect, segment), false)).collect(Collectors.toList()));
        IAnalysisModule moduleFutex = getFutexModule(getTrace());
        if (!(moduleFutex instanceof ISegmentStoreProvider)) {
            return ret;
        }
        ISegmentStoreProvider storeProvider = (ISegmentStoreProvider) moduleFutex;
        ISegmentStore<@NonNull ISegment> segmentStore = storeProvider.getSegmentStore();
        if (segmentStore != null) {
            ret.addAll(segmentStore.stream()
                    .filter(segment -> segment.toString().contains("WAKE")) //$NON-NLS-1$
                    .map(segment -> new MarkerEvent(null, segment.getStart(), segment.getLength(), getMarkerTitle2(), CONTENTION_COLOR, getMarkerTitle2(), false)).collect(Collectors.toList()));
        }

        return ret;
    }

    private static String getMarkerTitle2() {
        return "Block"; //$NON-NLS-1$
    }

    private @Nullable static IAnalysisModule getFutexModule(ITmfTrace trace) {
        String analysisId = "futex analysis lttng"; //$NON-NLS-1$
        @Nullable
        IAnalysisModule analysisModule = trace.getAnalysisModule(analysisId);
        if (analysisModule == null) {
            return trace.getChildren(TmfTrace.class).stream().map(a -> a.getAnalysisModule((analysisId))).filter(elem -> elem != null).findAny().orElse(null);
        }
        return analysisModule; // $NON-NLS-1$
    }

    private static @NonNull String getMarkerTitle(ISegmentAspect aspect, @NonNull ISegment seg) {
        return "Potential block by " + String.valueOf(aspect.resolve(seg)); //$NON-NLS-1$
    }

}
