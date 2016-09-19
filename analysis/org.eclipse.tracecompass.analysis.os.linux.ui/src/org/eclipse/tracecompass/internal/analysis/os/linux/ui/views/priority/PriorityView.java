/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelThreadInformationProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfCpuSelectedSignal;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfThreadSelectedSignal;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Messages;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowCpuAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowThreadAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.UnfollowCpuAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority.PriorityViewEntry.AggregatePriorityEntry;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority.PriorityViewEntry.Type;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractStateSystemTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.Iterables;

/**
 * Main implementation for the priority view
 *
 * @author Matthew Khouzam
 */
public class PriorityView extends AbstractStateSystemTimeGraphView {

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.priority"; //$NON-NLS-1$

    /** ID of the followed CPU in the map data in {@link TmfTraceContext} */
    public static final @NonNull String RESOURCES_FOLLOW_CPU = ID + ".FOLLOW_CPU"; //$NON-NLS-1$
    public static final @NonNull String RESOURCES_FOLLOW_THREAD = ID + ".FOLLOW_THREAD"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.ResourcesView_stateTypeName
    };

    // Timeout between updates in the build thread in ms
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    private static final RGBA CONTENTION_COLOR = new RGBA(200, 100, 30, 70);

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public PriorityView() {
        super(ID, new PriorityPresentationProvider());
        setTreeColumns(new String[] { "", "TID" });
        setTreeLabelProvider(new TreeLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof PriorityViewEntry) {
                    PriorityViewEntry priorityEntry = (PriorityViewEntry) element;
                    if (columnIndex == 0) {
                        return priorityEntry.getName();
                    }
                    if (columnIndex == 1 && priorityEntry.getType() == PriorityViewEntry.Type.THREAD) {
                        return Integer.toString(priorityEntry.getId());
                    }
                    return ""; //$NON-NLS-1$
                }
                return "";
            }
        });
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new PriorityFilterLabelProvider());
        setEntryComparator(new PriorityEntryComparator());
        setAutoExpandLevel(1);
    }

    private static class PriorityEntryComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            TimeGraphEntry entry1 = (TimeGraphEntry) o1;
            TimeGraphEntry entry2 = (TimeGraphEntry) o2;
            if (entry1.getParent() == null && entry2.getParent() == null) {
                /* sort trace entries alphabetically */
                return entry1.getName().compareTo(entry2.getName());
            } else if (entry1 instanceof Comparable) {
                /* sort resource entries by their defined order */
                Comparable comparable = (Comparable) entry1;
                return comparable.compareTo(entry2);
            }
            return 0;
        }
    }

    /**
     * @since 2.0
     */
    @Override
    protected void fillTimeGraphEntryContextMenu(@NonNull IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection sSel = (IStructuredSelection) selection;
            if (sSel.getFirstElement() instanceof PriorityViewEntry) {
                PriorityViewEntry resourcesEntry = (PriorityViewEntry) sSel.getFirstElement();
                if (resourcesEntry.getType().equals(PriorityViewEntry.Type.CPU)) {
                    TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
                    Integer data = (Integer) ctx.getData(RESOURCES_FOLLOW_CPU);
                    int cpu = data != null ? data.intValue() : -1;
                    if (cpu >= 0) {
                        menuManager.add(new UnfollowCpuAction(PriorityView.this, resourcesEntry.getId(), resourcesEntry.getTrace()));
                    } else {
                        menuManager.add(new FollowCpuAction(PriorityView.this, resourcesEntry.getId(), resourcesEntry.getTrace()));
                    }
                }
                if (resourcesEntry.getType().equals(PriorityViewEntry.Type.THREAD)) {
                    int thread = resourcesEntry.getId();
                    KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(resourcesEntry.getTrace(), KernelAnalysisModule.class, KernelAnalysisModule.ID);
                    String execName = KernelThreadInformationProvider.getExecutableName(module, thread);
                    if (thread >= 0) {
                        menuManager.add(new FollowThreadAction(PriorityView.this, execName, resourcesEntry.getId(), resourcesEntry.getTrace()));
                    }
                }
            }
        }
    }

    private static class PriorityFilterLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            PriorityViewEntry entry = (PriorityViewEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            return ""; //$NON-NLS-1$
        }

    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected String getNextText() {
        return Messages.ResourcesView_nextResourceActionNameText;
    }

    @Override
    protected String getNextTooltip() {
        return Messages.ResourcesView_nextResourceActionToolTipText;
    }

    @Override
    protected String getPrevText() {
        return Messages.ResourcesView_previousResourceActionNameText;
    }

    @Override
    protected String getPrevTooltip() {
        return Messages.ResourcesView_previousResourceActionToolTipText;
    }

    @Override
    protected void buildEntryList(ITmfTrace trace, ITmfTrace parentTrace, final IProgressMonitor monitor) {
        final ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, KernelAnalysisModule.ID);
        if (ssq == null) {
            return;
        }

        Map<Integer, TimeGraphEntry> entryMap = new HashMap<>();
        TimeGraphEntry traceEntry = null;

        long startTime = ssq.getStartTime();
        long start = startTime;
        setStartTime(Math.min(getStartTime(), startTime));
        boolean complete = false;
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }
            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
            long end = ssq.getCurrentEndTime();
            if (start == end && !complete) {
                // when complete execute one last time regardless of end time
                continue;
            }
            long endTime = end + 1;
            setEndTime(Math.max(getEndTime(), endTime));

            if (traceEntry == null) {
                traceEntry = PriorityViewEntry.create(ITmfStateSystem.INVALID_ATTRIBUTE, trace, startTime, endTime, Type.TRACE, 0, null);
                List<TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
                addToEntryList(parentTrace, ssq, entryList);
            } else {
                traceEntry.updateEndTime(endTime);
            }
            List<Integer> cpuQuarks = ssq.getQuarks(Attributes.CPUS, "*"); //$NON-NLS-1$
            try {
                createCpuEntriesWithQuark(trace, ssq, entryMap, traceEntry, startTime, endTime, cpuQuarks);
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            } catch (StateSystemDisposedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (parentTrace.equals(getTrace())) {
                refresh();
            }
            final List<@NonNull TimeGraphEntry> traceEntryChildren = traceEntry.getChildren();
            final long resolution = Math.max(1, (endTime - ssq.getStartTime()) / getDisplayWidth());
            queryFullStates(ssq, ssq.getStartTime(), end, resolution, monitor, new IQueryHandler() {
                @Override
                public void handle(List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState) {
                    for (TimeGraphEntry child : traceEntryChildren) {
                        if (!populateEventsRecursively(fullStates, prevFullState, child).isOK()) {
                            return;
                        }
                    }
                }

                private IStatus populateEventsRecursively(@NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull TimeGraphEntry entry) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    List<ITimeEvent> eventList = getEventList(entry, ssq, fullStates, prevFullState, monitor);
                    if (eventList != null) {
                        /*
                         * Start a new event list on first iteration, then
                         * append to it
                         */
                        if (prevFullState == null) {
                            entry.setEventList(eventList);
                        } else {
                            for (ITimeEvent event : eventList) {
                                entry.addEvent(event);
                            }
                        }
                    }
                    for (TimeGraphEntry child : entry.getChildren()) {
                        IStatus status = populateEventsRecursively(fullStates, prevFullState, child);
                        if (!status.isOK()) {
                            return status;
                        }
                    }
                    return Status.OK_STATUS;
                }
            });

            start = end;
        }

    }

    private static void createCpuEntriesWithQuark(@NonNull ITmfTrace trace, final ITmfStateSystem ssq, Map<Integer, TimeGraphEntry> entryMap, TimeGraphEntry traceEntry, long startTime, long endTime, List<Integer> cpuQuarks)
            throws AttributeNotFoundException, StateSystemDisposedException {
        for (Integer cpuQuark : cpuQuarks) {
            final @NonNull String cpuName = ssq.getAttributeName(cpuQuark);
            int cpu = Integer.parseInt(cpuName);
            TimeGraphEntry cpuEntry = entryMap.get(cpuQuark);
            if (cpuEntry == null) {
                cpuEntry = PriorityViewEntry.create(cpuQuark, trace, startTime, endTime, Type.CPU, cpu, Type.CPU.toString() + " " + cpu);
                entryMap.put(cpuQuark, cpuEntry);
                traceEntry.addChild(cpuEntry);
            } else {
                cpuEntry.updateEndTime(endTime);
            }
            int currentThreadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
            createCpuPriorityEntryWithQuark(trace, ssq, entryMap, startTime, ssq.getCurrentEndTime(), cpuEntry, currentThreadQuark);
        }
    }

    private static void createCpuPriorityEntryWithQuark(@NonNull ITmfTrace trace, ITmfStateSystem ssq, Map<Integer, TimeGraphEntry> entryMap, long startTime, long endTime, TimeGraphEntry parentEntry,
            int currentThreadQuark) throws StateSystemDisposedException, AttributeNotFoundException {
        Iterator<ITmfStateInterval> iter = ssq.getIteratorOverQuark(currentThreadQuark, startTime, ssq.getCurrentEndTime());
        if (!(parentEntry instanceof PriorityViewEntry)) {
            return;
        }
        PriorityViewEntry cpuEntry = (PriorityViewEntry) parentEntry;
        while (iter.hasNext()) {
            ITmfStateInterval currentThreadInterval = iter.next();
            int currenthread = currentThreadInterval.getStateValue().unboxInt();
            if (currenthread <= 0) {
                continue;
            }
            String threadString = Attributes.buildThreadAttributeName(currenthread, cpuEntry.getId());
            int threadQuark = ssq.getQuarkAbsolute(Attributes.THREADS, threadString);
            int prioQ = ssq.getQuarkRelative(threadQuark, Attributes.PRIO);
            int prio = ssq.querySingleState(currentThreadInterval.getStartTime(), prioQ).getStateValue().unboxInt();
            KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
            String execName = KernelThreadInformationProvider.getExecutableName(module, currenthread);

            TimeGraphEntry prioEntry = entryMap.get(prio - cpuEntry.getId() * 256);
            if (prioEntry == null) {
                prioEntry = PriorityViewEntry.create(prioQ, trace, startTime, endTime, Type.PRIORITY, prio, null);
                entryMap.put(prio - cpuEntry.getId() * 256, prioEntry);
                cpuEntry.addChild(prioEntry);
            } else {
                prioEntry.updateEndTime(endTime);
            }
            boolean found = false;
            for (TimeGraphEntry child : prioEntry.getChildren()) {
                if (child instanceof PriorityViewEntry) {
                    PriorityViewEntry threadEntry = (PriorityViewEntry) child;
                    if (threadEntry.getQuark() == threadQuark) {
                        threadEntry.updateEndTime(endTime);
                        found = true;
                    }
                }
            }
            if (!found) {
                TimeGraphEntry threadEntry = PriorityViewEntry.create(threadQuark, trace, startTime, endTime, Type.THREAD, currenthread, execName);
                entryMap.put(threadQuark + cpuEntry.getId() * 65536, threadEntry);
                prioEntry.addChild(threadEntry);
                if (prioEntry instanceof AggregatePriorityEntry) {
                    AggregatePriorityEntry aggregatePriorityEntry = (AggregatePriorityEntry) prioEntry;
                    aggregatePriorityEntry.addContributor(threadEntry);

                }
            }
        }

    }

    @Override
    protected @Nullable List<ITimeEvent> getEventList(@NonNull TimeGraphEntry entry, ITmfStateSystem ssq,
            @NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull IProgressMonitor monitor) {
        if (entry instanceof PriorityViewEntry) {
            PriorityViewEntry priorityEntry = (PriorityViewEntry) entry;
            Type type = priorityEntry.getType();
            switch (type) {
            case CPU:
                return createCpuEventsList(entry, fullStates, prevFullState, monitor, priorityEntry.getQuark());
            case THREAD:
                return createTreadEventsList(entry, ssq, fullStates, prevFullState, monitor, priorityEntry.getId(), ((PriorityViewEntry) entry.getParent().getParent()).getQuark());
            case NULL:
                break;
            case PRIORITY:
                break;
            case TRACE:
                break;
            default:
                break;
            }
        }
        return Collections.EMPTY_LIST;
    }

    private static List<ITimeEvent> createTreadEventsList(@NonNull TimeGraphEntry entry, ITmfStateSystem ssq, @NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull IProgressMonitor monitor,
            int threadId, int cpuQuark) {
        List<ITimeEvent> eventList = null;
        if (!(entry instanceof PriorityViewEntry)) {
            return eventList;
        }
        PriorityViewEntry priorityEntry = (PriorityViewEntry) entry;
        int cpuStatusQuark;
        int currentThreadQuark;
        int threadQuark = priorityEntry.getQuark();
        int statusQuark;
        try {
            statusQuark = threadQuark;
            cpuStatusQuark = cpuQuark;
            currentThreadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
        } catch (AttributeNotFoundException e) {
            /*
             * The sub-attribute "status" is not available. May happen if the
             * trace does not have sched_switch events enabled.
             */
            return null;
        }
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        eventList = new ArrayList<>(fullStates.size());
        ITmfStateInterval lastInterval = prevFullState == null || statusQuark >= prevFullState.size() ? null : prevFullState.get(cpuStatusQuark);
        long lastStartTime = lastInterval == null ? -1 : lastInterval.getStartTime();
        long lastEndTime = lastInterval == null ? -1 : lastInterval.getEndTime() + 1;
        for (List<ITmfStateInterval> fullState : fullStates) {
            if (monitor.isCanceled()) {
                return null;
            }
            if (statusQuark >= fullState.size()) {
                /* No information on this CPU (yet?), skip it for now */
                continue;
            }
            ITmfStateInterval statusInterval = fullState.get(statusQuark);
            long time = statusInterval.getStartTime();
            if (time == lastStartTime) {
                continue;
            }
            long duration = statusInterval.getEndTime() - time + 1;
            if (!statusInterval.getStateValue().isNull() && fullState.get(currentThreadQuark).getStateValue().unboxInt() == threadId) {
                if (lastEndTime != time && lastEndTime != -1) {
                    eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                }
                ITmfStateInterval cpuStatusInterval = fullState.get(cpuStatusQuark);
                int status = cpuStatusInterval.getStateValue().unboxInt();
                eventList.add(new TimeEvent(entry, time, duration, status));
            } else if (isZoomThread) {
                eventList.add(new NullTimeEvent(entry, time, duration));
            }
            lastStartTime = time;
            lastEndTime = time + duration;
        }
        return eventList;
    }

    private static List<ITimeEvent> createCpuEventsList(ITimeGraphEntry entry, List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState, IProgressMonitor monitor, int quark) {
        List<ITimeEvent> eventList;
        int statusQuark= quark;
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        eventList = new ArrayList<>(fullStates.size());
        ITmfStateInterval lastInterval = prevFullState == null || statusQuark >= prevFullState.size() ? null : prevFullState.get(statusQuark);
        long lastStartTime = lastInterval == null ? -1 : lastInterval.getStartTime();
        long lastEndTime = lastInterval == null ? -1 : lastInterval.getEndTime() + 1;
        for (List<ITmfStateInterval> fullState : fullStates) {
            if (monitor.isCanceled()) {
                return null;
            }
            if (statusQuark >= fullState.size()) {
                /* No information on this CPU (yet?), skip it for now */
                continue;
            }
            ITmfStateInterval statusInterval = fullState.get(statusQuark);
            int status = statusInterval.getStateValue().unboxInt();
            long time = statusInterval.getStartTime();
            long duration = statusInterval.getEndTime() - time + 1;
            if (time == lastStartTime) {
                continue;
            }
            if (!statusInterval.getStateValue().isNull()) {
                if (lastEndTime != time && lastEndTime != -1) {
                    eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                }
                eventList.add(new TimeEvent(entry, time, duration, status));
            } else if (isZoomThread) {
                eventList.add(new NullTimeEvent(entry, time, duration));
            }
            lastStartTime = time;
            lastEndTime = time + duration;
        }
        return eventList;
    }

    /**
     * Signal handler for a cpu selected signal.
     *
     * @param signal
     *            the cpu selected signal
     * @since 2.0
     */
    @TmfSignalHandler
    public void listenToCpu(TmfCpuSelectedSignal signal) {
        int data = signal.getCore() >= 0 ? signal.getCore() : -1;
        TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
        ctx.setData(RESOURCES_FOLLOW_CPU, data);
    }

    /**
     * Signal handler for a cpu selected signal.
     *
     * @param signal
     *            the cpu selected signal
     * @since 2.0
     */
    @TmfSignalHandler
    public void listenToThread(TmfThreadSelectedSignal signal) {
        int data = signal.getThreadId() >= 0 ? signal.getThreadId() : -1;
        TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
        ctx.setData(RESOURCES_FOLLOW_THREAD, data);
    }

    @Override
    protected @NonNull List<IMarkerEvent> getViewMarkerList(ITmfStateSystem ss,
            @NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull IProgressMonitor monitor) {
        IAnalysisModule module = getFutexModule(getTrace());
        List<IMarkerEvent> ret = new ArrayList<>();
        ret.addAll(super.getViewMarkerList(ss, fullStates, prevFullState, monitor));
        if (!(module instanceof ISegmentStoreProvider)) {
            return ret;
        }
        ISegmentStoreProvider storeProvider = (ISegmentStoreProvider) module;
        ISegmentAspect aspect = Iterables.<@Nullable ISegmentAspect> getFirst(storeProvider.getSegmentAspects(), null);
        ISegmentStore<@NonNull ISegment> segmentStore = storeProvider.getSegmentStore();
        if (aspect == null || segmentStore == null) {
            return ret;
        }
        ret.addAll(segmentStore.stream().map(segment -> new MarkerEvent(null, segment.getStart(), segment.getLength(), getMarkerTitle(aspect, segment), CONTENTION_COLOR, getMarkerTitle(aspect, segment), false))
                .collect(Collectors.toList()));
        return ret;
    }

    protected @Nullable IAnalysisModule getFutexModule(ITmfTrace trace) {
        return trace.getAnalysisModule("FUTEX ID");
    }

    private static String getMarkerTitle(ISegmentAspect aspect, @NonNull ISegment segment) {
        return "Contention " + String.valueOf(aspect.resolve(segment));
    }

}
