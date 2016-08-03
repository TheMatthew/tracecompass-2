package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.dialog;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * @author Jean-Christian Kouame
 * @since 1.1
 *
 */
public class SegmentFilterPredicate implements Predicate<@NonNull ISegment>{

    private Collection<org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter> fFilters;
    private boolean fIsMatchAny;

    /**
     * @param filters
     * @param isMatchAny
     */
    public SegmentFilterPredicate(Collection<org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter> filters, boolean isMatchAny) {
        fFilters = filters;
        fIsMatchAny = isMatchAny;
    }

    @Override
    public boolean test(@NonNull ISegment t) {
        Stream<org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter> activeFilters = fFilters.stream().filter(filter -> filter.isActive());
        Stream<org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ISegmentFilter> activeFiltersCopy = fFilters.stream().filter(filter -> filter.isActive());
        return !activeFilters.findAny().isPresent() ? true : fIsMatchAny ?
                activeFiltersCopy.anyMatch(filter -> filter.matches(t)) :
                    activeFiltersCopy.allMatch(filter -> filter.matches(t));
    }

}
