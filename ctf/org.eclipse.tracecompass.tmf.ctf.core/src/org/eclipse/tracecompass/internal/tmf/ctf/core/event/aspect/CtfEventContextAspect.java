/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ctf.core.event.aspect;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventField;

/**
 * Stream context event aspect for CTF traces.
 *
 * @author Matthew Khouzam
 */
public class CtfEventContextAspect implements ITmfEventAspect<@Nullable CtfTmfEventField> {

    @Override
    public CtfTmfEventField resolve(ITmfEvent event) {
        if (!(event instanceof CtfTmfEvent)) {
            return null;
        }
        ICompositeDefinition eventContext = ((CtfTmfEvent) event).getEventContext();
        if (eventContext == null) {
            return null;
        }
        return CtfTmfEventField.parseField(eventContext, "Context.Event");
    }

    @Override
    public @NonNull String getName() {
        // TODO Auto-generated method stub
        return "Event Context";
    }

    @Override
    public @NonNull String getHelpText() {
        return "The per-event context of a trace. This means that the data is stored in a given place for all events.";
    }
}
