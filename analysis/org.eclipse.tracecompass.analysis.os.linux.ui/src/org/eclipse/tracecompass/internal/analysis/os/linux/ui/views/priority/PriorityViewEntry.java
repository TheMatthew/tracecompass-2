/*******************************************************************************
 * Copyright (c) 2012, 2015 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Geneviève Bastien - Move code to provide base classes for time graph view
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.priority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.resources.AggregateEventIterator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry, or row, in the resource view
 *
 * @author Patrick Tasse
 */
public class PriorityViewEntry extends TimeGraphEntry implements Comparable<ITimeGraphEntry> {

    /** Type of resource */
    public static enum Type {
        /** Null resources (filler rows, etc.) */
        NULL,
        /** Entries for CPUs */
        CPU,
        /** Entries for Prioritys */
        PRIORITY,
        /** Entries for Threads */
        THREAD,
        /** entries for the top level trace */
        TRACE
    }

    private final int fId;
    private final @NonNull ITmfTrace fTrace;
    private final Type fType;
    private final int fQuark;

    /**
     * Constructor
     *
     * @param quark
     *            The attribute quark matching the entry
     * @param trace
     *            The trace on which we are working
     * @param name
     *            The exec_name of this entry
     * @param startTime
     *            The start time of this entry lifetime
     * @param endTime
     *            The end time of this entry
     * @param type
     *            The type of this entry
     * @param id
     *            The id of this entry
     */
    private PriorityViewEntry(int quark, @NonNull ITmfTrace trace, String name,
            long startTime, long endTime, Type type, int id) {
        super(name, startTime, endTime);
        fId = id;
        fTrace = trace;
        fType = type;
        fQuark = quark;
    }

    /**
     * Constructor
     *
     * @param quark
     *            The attribute quark matching the entry
     * @param trace
     *            The trace on which we are working
     * @param startTime
     *            The start time of this entry lifetime
     * @param endTime
     *            The end time of this entry
     * @param type
     *            The type of this entry
     * @param id
     *            The id of this entry
     * @param execName The executable name
     * @return the entry
     */
    public static TimeGraphEntry create(int quark, @NonNull ITmfTrace trace,
            long startTime, long endTime, Type type, int id, String execName) {
        switch (type) {
        case CPU:
            return new PriorityViewEntry(quark, trace, execName, startTime, endTime, type, id);
        case NULL:
            return new NullEntry(startTime, endTime);
        case PRIORITY:
            return new AggregatePriorityEntry(trace, startTime, endTime, id);
        case THREAD:
            return new PriorityViewEntry(quark, trace, execName, startTime, endTime, type, id);
        case TRACE:
            return new PriorityViewEntry(quark, trace, trace.getName(), startTime, endTime, type, id);
        default:
            throw new IllegalStateException("Type must be Priority, Cpu or Thread, not " + type.toString());
        }
    }

    /**
     * Get the entry's id
     *
     * @return the entry's id
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the entry's trace
     *
     * @return the entry's trace
     */
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the entry Type of this entry. Uses the inner Type enum.
     *
     * @return The entry type
     */
    public Type getType() {
        return fType;
    }

    /**
     * Retrieve the attribute quark that's represented by this entry.
     *
     * @return The integer quark The attribute quark matching the entry
     */
    public int getQuark() {
        return fQuark;
    }

    @Override
    public boolean hasTimeEvents() {
        if (fType == Type.NULL) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ITimeGraphEntry other) {
        if (!(other instanceof PriorityViewEntry)) {
            /*
             * Should not happen, but if it does, put those entries at the end
             */
            return -1;
        }
        PriorityViewEntry o = (PriorityViewEntry) other;

        /*
         * Resources entry names should all be of type "ABC 123"
         *
         * We want to filter on the Type first (the "ABC" part), then on the ID
         * ("123") in numerical order (so we get 1,2,10 and not 1,10,2).
         */
        int ret = this.getType().compareTo(o.getType());
        if (ret != 0) {
            return ret;
        }
        return Integer.compare(this.getId(), o.getId());
    }

    @Override
    public Iterator<@NonNull ITimeEvent> getTimeEventsIterator() {
        return super.getTimeEventsIterator();
    }

    static class NullEntry extends TimeGraphEntry {
        public NullEntry(long startTime, long endTime) {
            super(null, startTime, endTime);
        }
    }

    public static class AggregatePriorityEntry extends PriorityViewEntry {

        private final @NonNull List<ITimeGraphEntry> fContributors = new ArrayList<>();

        private static final Comparator<ITimeEvent> COMPARATOR = new Comparator<ITimeEvent>() {
            @Override
            public int compare(ITimeEvent o1, ITimeEvent o2) {
                // largest value
                return Integer.compare(getValue(o2), getValue(o1));
            }

            private int getValue(ITimeEvent element) {
                return (element instanceof TimeEvent) ? ((TimeEvent) element).getValue() : Integer.MIN_VALUE;
            }
        };

        private final int fPriority;

        /**
         * AggregateResourcesEntry Constructor
         *
         * @param trace
         *            the parent trace
         * @param startTime
         *            the start time
         * @param endTime
         *            the end time
         * @param type
         *            the type
         * @param id
         *            the id
         */
        public AggregatePriorityEntry(@NonNull ITmfTrace trace,
                long startTime, long endTime, int priority) {
            super(-1, trace, "Priority " + priority, startTime, endTime, Type.PRIORITY, priority);
            fPriority = priority;

        }

        @Override
        public void addEvent(ITimeEvent event) {
        }

        @Override
        public void addZoomedEvent(ITimeEvent event) {
        }

        @Override
        public Iterator<@NonNull ITimeEvent> getTimeEventsIterator() {
            return new AggregateEventIterator(fContributors, COMPARATOR);
        }

        @Override
        public Iterator<@NonNull ITimeEvent> getTimeEventsIterator(long startTime, long stopTime, long visibleDuration) {
            return new AggregateEventIterator(fContributors, startTime, stopTime, visibleDuration, COMPARATOR);
        }

        public void addContributor(ITimeGraphEntry entry) {
            fContributors.add(entry);
        }

        public int getPriority() {
            return fPriority;
        }
    }

}
