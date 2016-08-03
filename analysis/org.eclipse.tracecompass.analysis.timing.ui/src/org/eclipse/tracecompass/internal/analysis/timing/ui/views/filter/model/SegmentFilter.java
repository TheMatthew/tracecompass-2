package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * @author Jean-Christian Kouame
 * @since 1.1
 *
 */
public class SegmentFilter extends TmfFilterTreeNode {

    public static final String NODE_NAME = "SEGMENT_FILTER";
    public static final String PROVIDER_ID_ATTR = "provider_id";
    public static final String MATCH_ANY_ATTR = "match_any";
    private boolean matchAny = false;
    private String fSegmentProviderId;

    public SegmentFilter(@Nullable ITmfFilterTreeNode parent) {
        super(parent);
    }

    public boolean isMatchAny() {
        return matchAny;
    }

    public void setMatchAny(boolean matchAny) {
        this.matchAny = matchAny;
    }

    public String getSegmentProviderId() {
        return fSegmentProviderId;
    }

    public void setSegmentProviderId(String segmentProviderId) {
        fSegmentProviderId = segmentProviderId;
    }

    @Override
    public String toString(boolean explicit) {
        return "Segment Filter For " + NonNullUtils.nullToEmptyString(fSegmentProviderId);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setActive(boolean isActive) {
    }

    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public boolean matches(ISegment segment) {
        @NonNull Stream<@NonNull ITmfFilterTreeNode> filters1 = Arrays.asList(getChildren()).stream().filter(t -> t.isActive());
        @NonNull Stream<@NonNull ITmfFilterTreeNode> filters2 = Arrays.asList(getChildren()).stream().filter(t -> t.isActive());
        return !filters1.findAny().isPresent() ? true
                : matchAny ? filters2.anyMatch(t -> t.matches(segment))
                : filters2.allMatch(t -> t.matches(segment));
    }

    public void setChildren(@NonNull Collection<ISegmentFilter> children) {
        for (ISegmentFilter child : children) {
            addChild((ITmfFilterTreeNode)child);
        }
    }


}
