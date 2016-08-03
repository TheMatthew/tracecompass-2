package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.xml.SegmentFilterXMLParser;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.xml.SegmentFilterXMLWriter;
import org.xml.sax.SAXException;

/**
 * @author Jean-Christian Kouame
 *
 */
public class FilterManager {

    private static FilterManager fInstance;

    private static final String SAVED_FILTERS_FILE_NAME = "saved_filters.xml"; //$NON-NLS-1$
    private static final String SAVED_FILTERS_PATH_NAME =
        Activator.getDefault().getStateLocation().addTrailingSeparator().append(SAVED_FILTERS_FILE_NAME).toString();

    /*
     * Legacy path to the XML definitions file (in Linux Tools)
     *  TODO Remove once we feel the transition phase is over.
     */
    private static final IPath SAVED_FILTERS_FILE_NAME_LEGACY =
            Activator.getDefault().getStateLocation().removeLastSegments(1)
                    .append("org.eclipse.linuxtools.tmf.ui") //$NON-NLS-1$
                    .append(SAVED_FILTERS_FILE_NAME);

    private static ITmfFilterTreeNode fRoot = new TmfFilterRootNode();

    private FilterManager () {

        File defaultFile = new File(SAVED_FILTERS_PATH_NAME);

        try {
            /*
             * If there is no file at the expected location, check the legacy
             * location instead.
             */
            if (!defaultFile.exists()) {
                File legacyFileCore = SAVED_FILTERS_FILE_NAME_LEGACY.toFile();
                if (legacyFileCore.exists()) {
                    ITmfFilterTreeNode root = new SegmentFilterXMLParser(SAVED_FILTERS_FILE_NAME_LEGACY.toString()).getTree();
                    setSavedFilters(root.getChildren());
                }
            }
        } catch (FileNotFoundException e) {
        } catch (SAXException e) {
            Activator.getDefault().logError("Error parsing saved filter xml file: " + SAVED_FILTERS_FILE_NAME_LEGACY, e); //$NON-NLS-1$
        } catch (IOException e) {
            Activator.getDefault().logError("Error parsing saved filter xml file: " + SAVED_FILTERS_FILE_NAME_LEGACY, e); //$NON-NLS-1$
        }

        try {
            // Now load the filters from the current location
            fRoot = new SegmentFilterXMLParser(SAVED_FILTERS_PATH_NAME).getTree();
        } catch (FileNotFoundException e) {
        } catch (SAXException e) {
            Activator.getDefault().logError("Error parsing saved filter xml file: " + SAVED_FILTERS_PATH_NAME, e); //$NON-NLS-1$
        } catch (IOException e) {
            Activator.getDefault().logError("Error parsing saved filter xml file: " + SAVED_FILTERS_PATH_NAME, e); //$NON-NLS-1$
        }
    }
    /**
     * @return
     */
    public static synchronized FilterManager getInstance() {
        if (fInstance == null || !fRoot.hasChildren()) {
            fInstance = new FilterManager();
        }
        return fInstance;
    }

    /**
     * Retrieve the currently saved filters
     *
     * @return The array of filters
     */
    public @NonNull ITmfFilterTreeNode[] getSavedFilters() {
        return NonNullUtils.checkNotNull(fRoot.clone()).getChildren();
    }

    /**
     * Retrieve the currently saved filters
     *
     * @return The array of filters
     */
    public @Nullable ITmfFilterTreeNode getSavedFiltersByProviderId(String id) {
        for (ITmfFilterTreeNode node : fRoot.getChildren()) {
            if (node.getNodeName().equals(SegmentFilter.NODE_NAME)) {
                if (((SegmentFilter)node).getSegmentProviderId().equals(id)) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Set the passed filters as the currently saved ones.
     *
     * @param filters
     *            The filters to save
     */
    public void setSavedFilters(ITmfFilterTreeNode[] filters) {
        if (fRoot != null) {
            ITmfFilterTreeNode root = fRoot.clone();
            if (root != null) {
                for (ITmfFilterTreeNode filter : filters) {
                    if (filter.getNodeName().equals(SegmentFilter.NODE_NAME)) {
                        SegmentFilter sfFilter = (SegmentFilter) filter;
                        boolean exist = false;
                        for (int i = 0; i<root.getChildrenCount(); i++) {
                            ITmfFilterTreeNode child = root.getChild(i);
                            if (child.getNodeName().equals(SegmentFilter.NODE_NAME)) {
                                SegmentFilter sf = (SegmentFilter) child;
                                if (sfFilter.getSegmentProviderId().equals(sf.getSegmentProviderId())) {
                                    root.replaceChild(i, filter);
                                    exist = true;
                                }
                            }
                        }
                        if (!exist) {
                            root.addChild(filter);
                        }
                    }
                }
                fRoot = root.clone();
            }
        } else {
            fRoot = new TmfFilterRootNode();
            for (ITmfFilterTreeNode filter : filters) {
                fRoot.addChild(filter.clone());
            }
        }
        try {
            SegmentFilterXMLWriter writerXML = new SegmentFilterXMLWriter(fRoot);
            writerXML.saveTree(SAVED_FILTERS_PATH_NAME);
        } catch (ParserConfigurationException e) {
            Activator.getDefault().logError("Error saving filter xml file: " + SAVED_FILTERS_PATH_NAME, e); //$NON-NLS-1$
        }
    }

    public SegmentFilter getSegmentFilter(String fSegmentProviderId) {
        return (SegmentFilter) getSavedFiltersByProviderId(fSegmentProviderId);
    }

    public void setSegmentFilter(ISegmentFilter filter) {
        if (filter != null) {
            setSavedFilters(new ITmfFilterTreeNode[] {(ITmfFilterTreeNode)filter});
        }
    }
}
