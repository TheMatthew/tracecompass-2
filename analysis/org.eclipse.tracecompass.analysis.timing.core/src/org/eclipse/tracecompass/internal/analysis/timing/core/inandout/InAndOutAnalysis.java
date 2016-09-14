package org.eclipse.tracecompass.internal.analysis.timing.core.inandout;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

import com.google.common.collect.Iterables;

public class InAndOutAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    private static final Map<String, IInAndOutHandler> HANDLERS = new HashMap<>();
    private List<@NonNull ISegment> fMarkers = new ArrayList<>();
    private Collection<@NonNull ISegment> fExposedMarker = Collections.unmodifiableList(fMarkers);

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.inandout.analysis"; //$NON-NLS-1$

    @Override
    protected @NonNull AbstractSegmentStoreAnalysisRequest createAnalysisRequest(@NonNull ISegmentStore<@NonNull ISegment> segmentStore) {
        return new FS2AnalysisRequest(segmentStore);
    }

    @Override
    public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
        List<@NonNull ISegmentAspect> aspects = new ArrayList<>();
        Iterables.addAll(aspects, super.getSegmentAspects());
        aspects.add(new ISegmentAspect() {

            @Override
            public @Nullable Object resolve(@NonNull ISegment segment) {
                if (segment instanceof InAndOutSegment) {
                    return ((InAndOutSegment) segment).getName();
                }
                return null;
            }

            @Override
            public @NonNull String getName() {
                return "Type"; //$NON-NLS-1$
            }

            @Override
            public @NonNull String getHelpText() {
                return "Type, a pair of events minus the _begin and _end"; //$NON-NLS-1$
            }

            @Override
            public @Nullable Comparator<?> getComparator() {

                return new Comparator<ISegment>() {
                    @Override
                    public int compare(@Nullable ISegment o1, @Nullable ISegment o2) {
                        if (!(o1 instanceof InAndOutSegment) || !(o2 instanceof InAndOutSegment)) {
                            throw new IllegalArgumentException();
                        }
                        InAndOutSegment fs21 = (InAndOutSegment) o1;
                        InAndOutSegment fs22 = (InAndOutSegment) o2;
                        return fs21.getName().compareToIgnoreCase(fs22.getName());
                    }
                };
            }
        });
        return aspects;
    }

    private class FS2AnalysisRequest extends AbstractSegmentStoreAnalysisRequest {

        private static final String BEGIN_SUFFIX = "_begin";

        private static final String END_SUFFIX = "_end";

        private final Map<String, ITmfTimestamp> fOngoingFunctions = new HashMap<>();

        private final IProgressMonitor fMonitor = new NullProgressMonitor();

        public FS2AnalysisRequest(ISegmentStore<@NonNull ISegment> segmentStore) {
            super(segmentStore);
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);

            final String eventName = event.getName();

            IInAndOutHandler handler = HANDLERS.get(eventName);
            if (handler == null) {
                int beginIndex = eventName.indexOf(':') + 1;
                if (eventName.endsWith(BEGIN_SUFFIX)) {
                    HANDLERS.put(eventName, new GenericInAndOutHandler.Begin(eventName.substring(beginIndex, eventName.length() - BEGIN_SUFFIX.length())));
                } else if (eventName.endsWith(END_SUFFIX)) {
                    HANDLERS.put(eventName, new GenericInAndOutHandler.End(eventName.substring(beginIndex, eventName.length() - END_SUFFIX.length())));
                }
                handler = HANDLERS.get(eventName);
            }
            if (handler != null) {
                handler.handle(event, fOngoingFunctions, getSegmentStore(), fMarkers);
            }
        }

        @Override
        public void handleCompleted() {
            fOngoingFunctions.clear();
            super.handleCompleted();
        }

        @Override
        public void handleCancel() {
            fMonitor.setCanceled(true);
            super.handleCancel();
        }
    }

    @Override
    protected Object @NonNull [] readObject(@NonNull ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

    public Collection<@NonNull ISegment> getMarkers() {
        return fExposedMarker;
    }

}
