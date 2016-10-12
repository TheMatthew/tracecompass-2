/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

/**
 * Color registry, gets RGBs for given names
 *
 * @author Matthew Khouzam
 * @since 2.2
 *
 */
public final class TmfColorRegistry {

    private static final TmfColorRegistry INSTANCE = new TmfColorRegistry();

    private final Map<String, RGBA> fColorMap = new HashMap<>();

    private TmfColorRegistry() {
        // do nothing. later load from disk maybe?
    }

    /**
     * Get the instance
     *
     * @return the instance
     */
    public static TmfColorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Get the color
     *
     * @param colorName
     *            the name of the color
     * @return the color or null if none is set
     */
    public RGBA getColorRGBA(String colorName) {
        return fColorMap.get(colorName);
    }

    /**
     * Register a color
     * @param key the color name
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public RGBA register(String key, int r, int g, int b, int a) {
        return fColorMap.put(key, new RGBA(r, g, b, a));
    }

    public RGB getColor(String colorName) {
        RGBA color = getColorRGBA(colorName);
        if( colorName.equals("WAIT_FOR_CPU")){
            new Object();
        }
        if (color == null) {
            return null;
        }
        return color.rgb;
    }

    public Set<RGBA> getRegisteredColors(){
        return fColorMap.values().stream().collect(Collectors.toSet());
    }

    public Set<String> getRegisteredColorNames(){
        return fColorMap.keySet().stream().collect(Collectors.toSet());
    }
}
