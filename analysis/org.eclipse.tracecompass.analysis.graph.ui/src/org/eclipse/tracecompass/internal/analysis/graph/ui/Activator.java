/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.tmf.ui.util.TmfColorRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 *
 * @author Geneviève Bastien
 */
public class Activator extends AbstractUIPlugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.analysis.graph.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        TmfColorRegistry.getInstance().register("RUNNING", 0x33, 0x99, 0x00, 0xff); //$NON-NLS-1$
        /** Worker is interrupted */
        TmfColorRegistry.getInstance().register("INTERRUPTED", 0xff, 0xdc, 0x00, 0xff); //$NON-NLS-1$
        /** Worker has been preempted */
        TmfColorRegistry.getInstance().register("PREEMPTED", 0xc8, 0x64, 0x00, 0xff); //$NON-NLS-1$
        /** Worker waiting on a timer */
        TmfColorRegistry.getInstance().register("TIMER", 0x33, 0x66, 0x99, 0xff); //$NON-NLS-1$
        /** Worker is blocked, waiting on a device */
        TmfColorRegistry.getInstance().register("BLOCK_DEVICE", 0x66, 0x00, 0xcc, 0xff); //$NON-NLS-1$
        /** Worker is waiting for user input */
        TmfColorRegistry.getInstance().register("USER_INPUT", 0x5a, 0x01, 0x01, 0xff); //$NON-NLS-1$
        /** Worker is waiting on network */
        TmfColorRegistry.getInstance().register("NETWORK", 0xff, 0x9b, 0xff, 0xff); //$NON-NLS-1$
        /** Worker is waiting for an IPI */
        TmfColorRegistry.getInstance().register("IPI", 0x66, 0x66, 0xcc, 0xff); //$NON-NLS-1$
        /** Any other reason */
        TmfColorRegistry.getInstance().register("UNKNOWN", 0x40, 0x3b, 0x33, 0xff); //$NON-NLS-1$


    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    // --------------------------------------
    // Log functions
    // --------------------------------------

    /**
     * Logs a message with severity INFO in the runtime log of the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logInfo(String message) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity INFO in the runtime log of the
     * plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logInfo(String message, Throwable exception) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message, exception));
    }

    /**
     * Logs a message and exception with severity WARNING in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logWarning(String message) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity WARNING in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logWarning(String message, Throwable exception) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
    }

    /**
     * Logs a message and exception with severity ERROR in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logError(String message) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity ERROR in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logError(String message, Throwable exception) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }

}
