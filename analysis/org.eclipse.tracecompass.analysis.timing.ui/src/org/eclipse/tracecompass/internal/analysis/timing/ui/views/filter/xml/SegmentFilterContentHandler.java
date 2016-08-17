/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Yuriy Vashchuk (yvashchuk@gmail.com) - Initial API and implementation
 *       based on http://smeric.developpez.com/java/cours/xml/sax/
 *   Patrick Tasse - Refactoring
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.xml;

import java.util.Stack;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilter;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilterMatchesNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterAndNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterAspectNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterOrNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterRootNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterTreeNode;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The SAX Content Handler
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @author Patrick Tasse
 */
public class SegmentFilterContentHandler extends DefaultHandler {

    private ITmfFilterTreeNode fRoot = null;
    private Stack<ITmfFilterTreeNode> fFilterTreeStack = null;
    private Stack<Iterable<ISegmentAspect>> fAspectsStack = null;

    /**
     * The default constructor
     */
    public SegmentFilterContentHandler() {
        super();
        fFilterTreeStack = new Stack<>();
        fAspectsStack = new Stack<>();
    }

    /**
     * Getter of tree
     *
     * @return The builded tree
     */
    public ITmfFilterTreeNode getTree() {
        return fRoot;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        ITmfFilterTreeNode node = null;

        if (localName.equalsIgnoreCase(TmfFilterRootNode.NODE_NAME)) {

            node = new TmfFilterRootNode();

        } else if (localName.equalsIgnoreCase(SegmentFilter.NODE_NAME)) {
            node = new SegmentFilter(null);
            String value = atts.getValue(SegmentFilter.PROVIDER_ID_ATTR);
            if (value != null) {
                ((SegmentFilter) node).setSegmentProviderId(value);
            }

            value = atts.getValue(SegmentFilter.MATCH_ANY_ATTR);
            if (value != null && value.equalsIgnoreCase(Boolean.TRUE.toString())) {
                ((SegmentFilter) node).setMatchAny(true);
            }

            final ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            if (trace != null) {
                // FIXME Segment store provider is not necessarily an analysis
                String segmentProviderId = ((SegmentFilter) node).getSegmentProviderId();
                @Nullable TmfAbstractAnalysisModule analysisModuleOfClass = TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfAbstractAnalysisModule.class, NonNullUtils.checkNotNull(segmentProviderId));
                if (!(analysisModuleOfClass instanceof ISegmentStoreProvider)) {
                    return;
                }
                @NonNull ISegmentStoreProvider provider = (ISegmentStoreProvider) analysisModuleOfClass;
                fAspectsStack.push(provider.getSegmentAspects());
            }
        } else if (localName.equals(TmfFilterNode.NODE_NAME)) {

            node = new TmfFilterNode(atts.getValue(TmfFilterNode.NAME_ATTR));

        } else if (localName.equals(TmfFilterAndNode.NODE_NAME)) {

            node = new TmfFilterAndNode(null);
            String value = atts.getValue(TmfFilterAndNode.NOT_ATTR);
            if (value != null && value.equalsIgnoreCase(Boolean.TRUE.toString())) {
                ((TmfFilterAndNode) node).setNot(true);
            }

        } else if (localName.equals(TmfFilterOrNode.NODE_NAME)) {

            node = new TmfFilterOrNode(null);
            String value = atts.getValue(TmfFilterOrNode.NOT_ATTR);
            if (value != null && value.equalsIgnoreCase(Boolean.TRUE.toString())) {
                ((TmfFilterOrNode) node).setNot(true);
            }

        } else if (localName.equals(SegmentFilterMatchesNode.NODE_NAME)) {

            node = new SegmentFilterMatchesNode(null);
            String value = atts.getValue(SegmentFilterMatchesNode.NOT_ATTR);
            if (value != null && value.equalsIgnoreCase(Boolean.TRUE.toString())) {
                ((SegmentFilterMatchesNode) node).setNot(true);
            }
            if (fAspectsStack.isEmpty()) {
                return;
            }
            Iterable<ISegmentAspect> aspects = fAspectsStack.peek();
            createEventAspect((TmfFilterAspectNode) node, atts, aspects);
            ((SegmentFilterMatchesNode) node).setRegex(atts.getValue(SegmentFilterMatchesNode.REGEX_ATTR));
        }

        fFilterTreeStack.push(node);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (fFilterTreeStack.isEmpty()) {
            return;
        }
        ITmfFilterTreeNode node = fFilterTreeStack.pop();

        if (node.getNodeName().equals(SegmentFilter.NODE_NAME)) {
            fAspectsStack.pop();
        }

        if (fFilterTreeStack.isEmpty()) {
            fRoot = node;
        } else if (fFilterTreeStack.lastElement() instanceof TmfFilterTreeNode &&
                node instanceof TmfFilterTreeNode) {
            fFilterTreeStack.lastElement().addChild(node);
        }

    }

    private static void createEventAspect(TmfFilterAspectNode node, Attributes atts, Iterable<ISegmentAspect> aspects) {
        String name = atts.getValue(TmfFilterAspectNode.SEGMENT_ASPECT_ATTR);
        if (aspects != null) {
            for (ISegmentAspect aspect : aspects) {
                if (aspect.getName().equals(name)) {
                    node.setEventAspect(aspect);
                    return;
                }
            }
        }
        node.setEventAspect(null);
    }
}
