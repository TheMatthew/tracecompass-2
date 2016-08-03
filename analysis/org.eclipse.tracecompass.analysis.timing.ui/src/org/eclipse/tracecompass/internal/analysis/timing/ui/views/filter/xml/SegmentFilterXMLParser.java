/*******************************************************************************
 * Copyright (c) 2010, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Yuriy Vashchuk (yvashchuk@gmail.com) - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * The SAX based XML parser
 *
 * @version 1.0
 * @author Yuriy Vashchuk
 * @author Patrick Tasse
 * @since 1.1
 */
public class SegmentFilterXMLParser {

    private static ITmfFilterTreeNode fRoot = null;

    /**
     * The XMLParser constructor
     *
     * @param uri The XML file to parse
     * @throws SAXException  SAX exception
     * @throws IOException  IO exception
     */
    public SegmentFilterXMLParser(final String uri) throws SAXException, IOException {

        SAXParserFactory m_parserFactory = null;
        m_parserFactory = SAXParserFactory.newInstance();
        m_parserFactory.setNamespaceAware(true);

        XMLReader saxReader = null;
        try {

            saxReader = m_parserFactory.newSAXParser().getXMLReader();
            saxReader.setContentHandler(new SegmentFilterContentHandler());
            saxReader.parse(uri);

            fRoot = ((SegmentFilterContentHandler) saxReader.getContentHandler()).getTree();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Getter of tree
     *
     * @return The builded tree
     */
    public ITmfFilterTreeNode getTree() {
        return fRoot;
    }
}
