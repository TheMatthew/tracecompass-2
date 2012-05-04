/**********************************************************************
 * Copyright (c) 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.ui.views.control.service;

import java.util.regex.Matcher;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.messages.Messages;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.remote.ICommandResult;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.remote.ICommandShell;

/**
 * <p>
 * Factory to create LTTngControlService instances depending on the version of the LTTng Trace Control
 * installed on the remote host.
 * </p>
 * 
 * @author Bernd Hufmann
 */
public class LTTngControlServiceFactory {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The singleton instance.
     */
    private static LTTngControlServiceFactory fInstance = null;
    
    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------
    /**
     * Constructor
     */
    private LTTngControlServiceFactory() {
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    /**
     * @return the LTTngControlServiceFactory singleton instance.
     */
    public static synchronized LTTngControlServiceFactory getInstance() {
        if (fInstance == null) {
            fInstance = new LTTngControlServiceFactory();
        }
        return fInstance;
    }

    // ------------------------------------------------------------------------
    // Factory method
    // ------------------------------------------------------------------------
    /**
     * Gets the LTTng Control Service implementation based on the version of the 
     * remote LTTng Tools.
     * 
     * @param shell - the shell implementation to pass to the service
     * @return - LTTng Control Service implementation
     * @throws ExecutionException
     */
    public ILttngControlService getLttngControlService(ICommandShell shell) throws ExecutionException {
        // get the version
        ICommandResult result = shell.executeCommand(LTTngControlServiceConstants.CONTROL_COMMAND + LTTngControlServiceConstants.COMMAND_VERSION, new NullProgressMonitor());
        
        if ((result != null) && (result.getResult() == 0) && (result.getOutput().length >= 1) && (!LTTngControlServiceConstants.ERROR_PATTERN.matcher(result.getOutput()[0]).matches())) {
            int index = 0;
            while (index < result.getOutput().length) {
                String line = result.getOutput()[index];
                Matcher matcher = LTTngControlServiceConstants.VERSION_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String version = matcher.group(1).trim();
                    if (version.startsWith(LTTngControlServiceConstants.LTTNG_MAJOR_VERSION_2_0)) {
                        LTTngControlService service = new LTTngControlService(shell);
                        service.setVersion(version);
                        return service;
                    }
                    throw new ExecutionException(Messages.TraceControl_UnsupportedVersionError + ": " + version); //$NON-NLS-1$
                }
                index++;
            }
        }
        throw new ExecutionException(Messages.TraceControl_GettingVersionError);
    }
}
