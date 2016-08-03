/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

/**
 * Matches filter node for segment
 *
 * @author Jean-Christian Kouame
 * @since 1.1
 *
 */
public class SegmentFilterMatchesNode extends TmfFilterAspectNode {

    /** matches node name */
    public static final String NODE_NAME = "MATCHES"; //$NON-NLS-1$
    /** not attribute name */
    public static final String NOT_ATTR = "not"; //$NON-NLS-1$
    /** regex attribute name */
    public static final String REGEX_ATTR = "regex"; //$NON-NLS-1$

    private boolean fNot;

    private @Nullable String fRegex;
    private transient @Nullable Pattern fPattern;
    private boolean fCaseSensitive;
    private boolean fActive;

    /**
     * Constructor
     *
     * @param parent
     *            The parent node
     */
    public SegmentFilterMatchesNode(@Nullable ITmfFilterTreeNode parent) {
        super(parent);
        fActive = true;
    }

    /**
     * Get the not state
     *
     * @return the NOT state
     */
    public boolean isNot() {
        return fNot;
    }

    /**
     * @return the regular expression
     */
    public String getRegex() {
        return NonNullUtils.nullToEmptyString(fRegex);
    }

    /**
     * @param regex
     *            the regular expression
     */
    public void setRegex(String regex) {
        this.fRegex = regex;
        try {
            int flag = !fCaseSensitive ? Pattern.DOTALL : Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
            this.fPattern = Pattern.compile(regex, flag);
        } catch (PatternSyntaxException e) {
            this.fPattern = null;
        }
    }

    /**
     * Get the pattern
     *
     * @return the regex pattern
     */
    protected @Nullable Pattern getPattern() {
        return fPattern;
    }

    /**
     * The node name
     *
     * @return The name
     */
    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    /**
     * Set the ACTIVE state of the filter
     *
     * @param active
     *            The new ACTIVE state
     */
    @Override
    public void setActive(boolean active) {
        fActive = active;
    }

    /**
     * Tells the ACTIVE state of the filter
     *
     * @return The ACTIVE state
     */
    @Override
    public boolean isActive() {
        return fActive;
    }

    /**
     * Verify the filter conditions on a segment
     *
     * @param segment
     *            The segment
     * @return True if the segment matches the filter condition, false otherwise
     */
    @Override
    public boolean matches(ISegment segment) {
        Pattern pattern = getPattern();
        ISegmentAspect aspect = fEventAspect;
        boolean isNot = isNot();

        if (pattern == null || aspect == null) {
            return false ^ isNot;
        }

        Object value = aspect.resolve(segment);
        if (value == null) {
            return false ^ isNot;
        }
        String string = value.toString();
        return pattern.matcher(string).find() ^ isNot;
    }

    @Override
    public @NonNull String toString(boolean explicit) {
        return "'" + NonNullUtils.checkNotNull(fEventAspect).getName() + "'" + (fNot ? " not matches \"" : " matches \"") + getRegex() + '\"'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    /**
     * @param not
     */
    public void setNot(boolean not) {
        fNot = not;
    }
}
