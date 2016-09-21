/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.pattern.stateprovider;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * State system analysis for pattern matching analysis described in XML. This
 * module will parse the XML description of the analyses and execute it against
 * the trace and will execute all required action
 *
 * @author Jean-Christian Kouame
 */
public class XmlPatternStateSystemModule extends TmfStateSystemAnalysisModule {

    private @Nullable IPath fXmlFile;
    private final ISegmentListener fListener;
    private @Nullable XmlPatternStateProvider fProvider;

    /**
     * Constructor
     *
     * @param listener
     *            Listener for segments that will be created
     */
    public XmlPatternStateSystemModule(ISegmentListener listener) {
        super();
        fListener = listener;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        XmlPatternStateProvider provider = fProvider;
        if (provider == null) {
            provider = new XmlPatternStateProvider(checkNotNull(getTrace()), getId(), fXmlFile, fListener);
            fProvider = provider;
        }
        return provider;
    }

    /**
     * Sets the file path of the XML file containing the state provider
     *
     * @param file
     *            The full path to the XML file
     */
    public void setXmlFile(IPath file) {
        fXmlFile = file;
    }

    /**
     * Get the pattern state provider
     *
     * @return The provider
     */
    public XmlPatternStateProvider getProvider() {
        return (XmlPatternStateProvider) createStateProvider();
    }
}
