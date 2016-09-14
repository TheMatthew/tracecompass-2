package org.eclipse.tracecompass.internal.analysis.timing.core.inandout;

import org.eclipse.tracecompass.segmentstore.core.ISegment;

public class InAndOutSegment implements ISegment {

    /**
     * ID
     */
    private static final long serialVersionUID = -1486639321309880930L;
    private long fIn;
    private long fOut;
    private String fName;

    public InAndOutSegment(long in, long out, String name) {
        fIn = in;
        fOut = out;
        fName = name;
    }

    @Override
    public long getStart() {
        return fIn;
    }

    @Override
    public long getEnd() {
        return fOut;
    }

    public String getName() {
        return fName;
    }
}
