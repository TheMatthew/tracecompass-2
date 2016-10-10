/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.util.TmfColorRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.base.Joiner;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.analysis.os.linux.ui"; //$NON-NLS-1$

    /**
     * The shared instance
     */
    private static Activator plugin;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * The constructor
     */
    public Activator() {
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    // ------------------------------------------------------------------------
    // AbstractUIPlugin
    // ------------------------------------------------------------------------

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

        TmfColorRegistry.getInstance().register("RUNNING", 0x33, 0x99, 0x00, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("THREAD_USERMODE", 0x33, 0x99, 0x00, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("USERMODE", 0x33, 0x99, 0x00, 0xff); //$NON-NLS-1$
        /** Worker is interrupted */
        TmfColorRegistry.getInstance().register("THREAD_INTERRUPTED", 0xff, 0xdc, 0x00, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("IRQ", 0xff, 0xdc, 0x00, 0xff); //$NON-NLS-1$
        /** Worker has been preempted */
        TmfColorRegistry.getInstance().register("THREAD_WAIT_BLOCKED", 0xc8, 0x64, 0, 255); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("WAIT_BLOCKED", 0xc8, 0x64, 0, 255); //$NON-NLS-1$

        TmfColorRegistry.getInstance().register("THREAD_UNKNOWN", 0x40, 0x3b, 0x33, 0xff); //$NON-NLS-1$

        TmfColorRegistry.getInstance().register("UNKNOWN", 200, 200, 200, 255); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("WAIT_UNKNOWN", 200, 200, 200, 255); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("WAIT_FOR_CPU", 160, 160, 30, 255); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("THREAD_", 200, 100, 0, 255); //$NON-NLS-1$

        TmfColorRegistry.getInstance().register("IDLE", 200, 200, 200, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("THREAD_SYSCALL", 0, 0, 200, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("SYSCALL", 0, 0, 200, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("SOFT_IRQ", 200, 150, 100, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("IRQ_ACTIVE", 0xff, 0xdc, 0x00, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("SOFT_IRQ_RAISED", 200, 200, 0, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("SOFT_IRQ_ACTIVE", 200, 150, 100, 0xff); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("CONTENTION", 200, 100, 30, 70); //$NON-NLS-1$
        TmfColorRegistry.getInstance().register("WAIT_VMM", 200, 0, 0, 255);
        TmfColorRegistry.getInstance().register("VCPU_PREEMPTED", 120, 40, 90, 255);
        for (RGBA a : TmfColorRegistry.getInstance().getRegisteredColors()) {
            if (TmfColorRegistry.getInstance().getRegisteredColorNames().stream().filter(b -> a.equals(TmfColorRegistry.getInstance().getColorRGBA(b))).count() > 1) {
                List<String> list = TmfColorRegistry.getInstance().getRegisteredColorNames().stream().filter(b -> a.equals(TmfColorRegistry.getInstance().getColorRGBA(b))).collect(Collectors.toList());
                System.out.println(Joiner.on(", ").skipNulls().join(list));
            }
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Get the image object from a given path
     *
     * @param path
     *            The path to the image file
     * @return The Image object
     */
    public Image getImageFromPath(String path) {
        return getImageDescripterFromPath(path).createImage();
    }

    /**
     * Get the ImageDescriptor from a given path
     *
     * @param path
     *            The path to the image file
     * @return The ImageDescriptor object
     */
    public ImageDescriptor getImageDescripterFromPath(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Get the Image from a registry
     *
     * @param path
     *            The path to the image registry
     * @return The Image object
     */
    public Image getImageFromImageRegistry(String path) {
        Image icon = getImageRegistry().get(path);
        if (icon == null) {
            icon = getImageDescripterFromPath(path).createImage();
            plugin.getImageRegistry().put(path, icon);
        }
        return icon;
    }

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
