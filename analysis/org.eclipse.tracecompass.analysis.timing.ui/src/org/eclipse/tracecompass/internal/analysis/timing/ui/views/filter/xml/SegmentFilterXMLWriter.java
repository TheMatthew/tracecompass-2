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
 *   Patrick Tasse - Update filter nodes
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilter;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilterMatchesNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterAspectNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterNode;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The SAX based XML writer
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @author Patrick Tasse
 */
public class SegmentFilterXMLWriter {

    private Document document = null;

    /**
     * The XMLParser constructor
     *
     * @param root The tree root
        * @throws ParserConfigurationException if a DocumentBuilder
     *   cannot be created which satisfies the configuration requested.
     */
    public SegmentFilterXMLWriter(final ITmfFilterTreeNode root) throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        document = documentBuilder.newDocument();

        Element rootElement = document.createElement(root.getNodeName());
        document.appendChild(rootElement);

        for (ITmfFilterTreeNode node : root.getChildren()) {
            buildXMLTree(document, node, rootElement);
        }
    }

    /**
     * The Tree to XML parser
     *
     * @param document The XML document
     * @param treenode The node to write
     * @param parentElement The XML element of the parent
     */
    public static void buildXMLTree(final Document document, final ITmfFilterTreeNode treenode, Element parentElement) {
        Element element = document.createElement(treenode.getNodeName());

        if (treenode instanceof TmfFilterNode) {

            TmfFilterNode node = (TmfFilterNode) treenode;
            element.setAttribute(TmfFilterNode.NAME_ATTR, node.getFilterName());

        } else if (treenode instanceof SegmentFilter) {
                SegmentFilter node = (SegmentFilter) treenode;
                element.setAttribute(SegmentFilter.PROVIDER_ID_ATTR, node.getSegmentProviderId());
                element.setAttribute(SegmentFilter.MATCH_ANY_ATTR, Boolean.toString(node.isMatchAny()));
        } else if (treenode instanceof SegmentFilterMatchesNode) {

            SegmentFilterMatchesNode node = (SegmentFilterMatchesNode) treenode;
            element.setAttribute(SegmentFilterMatchesNode.NOT_ATTR, Boolean.toString(node.isNot()));
            setAspectAttributes(element, node);
            element.setAttribute(SegmentFilterMatchesNode.REGEX_ATTR, node.getRegex());

        }

        parentElement.appendChild(element);

        for (int i = 0; i < treenode.getChildrenCount(); i++) {
            buildXMLTree(document, treenode.getChild(i), element);
        }
    }

    private static void setAspectAttributes(Element element, TmfFilterAspectNode node) {
        @Nullable ISegmentAspect aspect = node.getEventAspect();
        if (aspect != null) {
            element.setAttribute(TmfFilterAspectNode.SEGMENT_ASPECT_ATTR, aspect.getName());
        }
    }

    /**
     * Save the tree
     *
     * @param uri The new Filter XML path
     */
    public void saveTree(final String uri) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        try {
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result =  new StreamResult(new File(uri));
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

}