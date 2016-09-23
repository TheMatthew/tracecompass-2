/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.markers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.markers.Marker.SplitMarker;
import org.eclipse.tracecompass.internal.tmf.ui.markers.Marker.WeightedMarker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * XML Parser for periodic marker configuration
 */
public class MarkerConfigXmlParser {

    /** Default marker configuration file URL */
    private static final String DEFAULT_MARKER_CONFIG_URL = "platform:/plugin/org.eclipse.tracecompass.tmf.ui/share/markers.xml"; //$NON-NLS-1$
    /** Marker configuration file name */
    public static final String MARKER_CONFIG_NAME = "markers.xml"; //$NON-NLS-1$
    /** Marker configuration file path */
    public static final IPath MARKER_CONFIG_PATH = Activator.getDefault().getStateLocation().addTrailingSeparator().append(MARKER_CONFIG_NAME);
    /** Marker configuration file */
    private static final File MARKER_CONFIG_FILE = MARKER_CONFIG_PATH.toFile();
    /** Default marker label */
    private static final String DEFAULT_LABEL = "%d"; //$NON-NLS-1$

    private static final @NonNull String ELLIPSIS = ".."; //$NON-NLS-1$

    private static final int ALPHA = 10;

    /**
     * Get the marker sets from the marker configuration file.
     *
     * @return the list of marker sets
     */
    public static @NonNull List<MarkerSet> getMarkerSets() {
        if (!MARKER_CONFIG_FILE.exists()) {
            try {
                File defaultConfigFile = new File(FileLocator.toFileURL(new URL(DEFAULT_MARKER_CONFIG_URL)).toURI());
                Files.copy(defaultConfigFile.toPath(), MARKER_CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (URISyntaxException | IOException e) {
                Activator.getDefault().logError("Error copying " + DEFAULT_MARKER_CONFIG_URL + " to " + MARKER_CONFIG_FILE.getAbsolutePath(), e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return parse(MARKER_CONFIG_FILE.getAbsolutePath());
    }

    /**
     * Parse a periodic marker configuration file
     *
     * @param path
     *            the path to the configuration file
     * @return the list of marker sets
     */
    public static @NonNull List<MarkerSet> parse(String path) {

        List<MarkerSet> markerSets = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            // The following allows xml parsing without access to the dtd
            db.setEntityResolver((publicId, systemId) -> {
                return new InputSource(new ByteArrayInputStream("".getBytes())); //$NON-NLS-1$
            });

            // The following catches xml parsing exceptions
            db.setErrorHandler(new ErrorHandler() {
                @Override
                public void error(SAXParseException saxparseexception) throws SAXException {}

                @Override
                public void warning(SAXParseException saxparseexception) throws SAXException {}

                @Override
                public void fatalError(SAXParseException saxparseexception) throws SAXException {
                    throw saxparseexception;
                }
            });

            File file = new File(path);
            if (!file.canRead()) {
                return markerSets;
            }
            Document doc = db.parse(file);

            Element root = doc.getDocumentElement();
            if (!root.getNodeName().equals(IMarkerConstants.MARKER_SETS)) {
                return markerSets;
            }

            NodeList markerSetsList = root.getElementsByTagName(IMarkerConstants.MARKER_SET);
            for (int i = 0; i < markerSetsList.getLength(); i++) {
                try {
                    Element markerSetElem = (Element) markerSetsList.item(i);
                    String name = markerSetElem.getAttribute(IMarkerConstants.NAME);
                    String id = markerSetElem.getAttribute(IMarkerConstants.ID);
                    MarkerSet markerSet = new MarkerSet(name, id);
                    List<Marker> markers = getMarkers(markerSetElem);
                    for (Marker marker : markers) {
                        markerSet.addMarker(marker);
                    }
                    markerSets.add(markerSet);
                } catch (IllegalArgumentException e) {
                    Activator.getDefault().logError("Error parsing " + path, e); //$NON-NLS-1$
                }
            }
            return markerSets;

        } catch (ParserConfigurationException | SAXException | IOException e) {
            Activator.getDefault().logError("Error parsing " + path, e); //$NON-NLS-1$
        }
        return markerSets;
    }

    private static List<Marker> getMarkers(Element markerSet) {
        List<Marker> markers = new ArrayList<>();
        NodeList markerList = markerSet.getElementsByTagName(IMarkerConstants.MARKER);
        for (int i = 0; i < markerList.getLength(); i++) {
            Element markerElem = (Element) markerList.item(i);
            String name = markerElem.getAttribute(IMarkerConstants.NAME);
            String label = parseLabel(markerElem.getAttribute(IMarkerConstants.LABEL));
            String id = markerElem.getAttribute(IMarkerConstants.ID);
            RGBA color = parseColor(markerElem.getAttribute(IMarkerConstants.COLOR));
            double period = parsePeriod(markerElem.getAttribute(IMarkerConstants.PERIOD));
            String unit = parseUnit(markerElem.getAttribute(IMarkerConstants.UNIT));
            Range<Long> range = parseRange(markerElem.getAttribute(IMarkerConstants.RANGE));
            long offset = parseOffset(markerElem.getAttribute(IMarkerConstants.OFFSET));
            RangeSet<Long> indexRange = parseRangeSet(markerElem.getAttribute(IMarkerConstants.INDEX));
            SplitMarker marker = new SplitMarker(name, label, id, color, period, unit, range, offset, indexRange);
            parseSubMarkers(markerElem, marker);
            markers.add(marker);
        }
        return markers;
    }

    private static void parseSubMarkers(Element marker, SplitMarker parent) {
        NodeList nodeList = marker.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(IMarkerConstants.SUBMARKER)) {
                Element subMarkerElem = (Element) node;
                String name = subMarkerElem.getAttribute(IMarkerConstants.NAME);
                String label = parseLabel(subMarkerElem.getAttribute(IMarkerConstants.LABEL));
                String id = subMarkerElem.getAttribute(IMarkerConstants.ID);
                RGBA color = parent.getColor();
                String colorAttr = subMarkerElem.getAttribute(IMarkerConstants.COLOR);
                if (!colorAttr.isEmpty()) {
                    color = parseColor(colorAttr);
                }
                String unit = parent.getUnit();
                long offset = parent.getOffset();
                String rangeAttr = subMarkerElem.getAttribute(IMarkerConstants.RANGE);
                if (!rangeAttr.isEmpty()) {
                    /* this is a split sub-marker */
                    Range<Long> range = parseRange(rangeAttr);
                    double period = parent.getPeriod() / (range.upperEndpoint() - range.lowerEndpoint() + 1);
                    RangeSet<Long> indexRange = parseRangeSet(subMarkerElem.getAttribute(IMarkerConstants.INDEX));
                    SplitMarker subMarker = new SplitMarker(name, label, id, color, period, unit, range, offset, indexRange);
                    parent.addSubMarker(subMarker);
                    parseSubMarkers(subMarkerElem, subMarker);
                } else {
                    /* this is a weighted sub-marker */
                    double period = parent.getPeriod();
                    WeightedMarker subMarker = new WeightedMarker(name, period, unit, offset);
                    parent.addSubMarker(subMarker);
                    parseSegments(subMarkerElem, subMarker);
                }
            }
        }
    }

    private static void parseSegments(Element marker, WeightedMarker parent) {
        NodeList nodeList = marker.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(IMarkerConstants.SEGMENT)) {
                Element segmentElem = (Element) node;
                String label = parseLabel(segmentElem.getAttribute(IMarkerConstants.LABEL));
                String id = segmentElem.getAttribute(IMarkerConstants.ID);
                RGBA color = null;
                String colorAttr = segmentElem.getAttribute(IMarkerConstants.COLOR);
                if (!colorAttr.isEmpty()) {
                    color = parseColor(colorAttr);
                }
                int length = Integer.parseInt(segmentElem.getAttribute(IMarkerConstants.LENGTH));
                MarkerSegment segment = new MarkerSegment(label, id, color, length);
                parent.addSegment(segment);
            }
        }
    }

    private static String parseLabel(String labelAttr) {
        if (labelAttr.isEmpty()) {
            return DEFAULT_LABEL;
        }
        return labelAttr;
    }

    private static RGBA parseColor(String colorAttr) {
        RGB rgb = null;
        if (colorAttr.charAt(0) == '#') {
            if (colorAttr.length() < 7) {
                throw new IllegalArgumentException("Invalid color: " + colorAttr); //$NON-NLS-1$
            }
            rgb = new RGB(Integer.valueOf(colorAttr.substring(1, 3), 16),
                    Integer.valueOf(colorAttr.substring(3, 5), 16),
                    Integer.valueOf(colorAttr.substring(5, 7), 16));
        } else {
            rgb = X11Color.toRGB(colorAttr);
            if (rgb == null) {
                throw new IllegalArgumentException("Unsupported color: " + colorAttr); //$NON-NLS-1$
            }
        }
        return new RGBA(rgb.red, rgb.green, rgb.blue, ALPHA);
    }

    private static double parsePeriod(String periodAttr) {
        double period = Double.parseDouble(periodAttr);
        if (period <= 0) {
            throw new IllegalArgumentException("Unsupported period: " + periodAttr); //$NON-NLS-1$
        }
        return period;
    }

    private static String parseUnit(String unitAttr) {
        if (Arrays.asList(IMarkerConstants.MS, IMarkerConstants.US, IMarkerConstants.NS, IMarkerConstants.CYCLES).contains(unitAttr)) {
            return unitAttr;
        }
        throw new IllegalArgumentException("Unsupported unit: " + unitAttr); //$NON-NLS-1$
    }

    private static Range<Long> parseRange(String rangeAttr) {
        int index = rangeAttr.indexOf(ELLIPSIS);
        if (index > 0) {
            long min = Long.parseLong(rangeAttr.substring(0, index));
            index += ELLIPSIS.length();
            if (index < rangeAttr.length()) {
                long max = Long.parseLong(rangeAttr.substring(index));
                return Range.closed(min, max);
            }
            return Range.atLeast(min);
        }
        if (index == 0) {
            index += ELLIPSIS.length();
            if (index < rangeAttr.length()) {
                long max = Long.parseLong(rangeAttr.substring(index));
                return Range.atMost(max);
            }
            return Range.all();
        }
        if (!rangeAttr.isEmpty()) {
            long val = Long.parseLong(rangeAttr);
            return Range.closed(val, val);
        }
        return Range.atLeast(0L);
    }

    private static RangeSet<Long> parseRangeSet(String rangeSetAttr) {
        if (rangeSetAttr.isEmpty()) {
            return ImmutableRangeSet.of(Range.all());
        }
        RangeSet<Long> rangeSet = TreeRangeSet.create();
        String[] ranges = rangeSetAttr.split(","); //$NON-NLS-1$
        if (ranges.length == 0) {
            rangeSet.add(Range.all());
        } else {
            for (String range : ranges) {
                rangeSet.add(parseRange(range));
            }
        }
        return rangeSet;
    }

    private static long parseOffset(String offset) {
        if (offset.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(offset);
    }
}
