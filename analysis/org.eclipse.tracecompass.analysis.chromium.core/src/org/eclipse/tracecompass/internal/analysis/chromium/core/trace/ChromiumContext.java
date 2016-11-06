package org.eclipse.tracecompass.internal.analysis.chromium.core.trace;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.Activator;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class ChromiumContext extends TmfContext {

    private final JsonParser fParser;

    public ChromiumContext(JsonParser parser) {
        fParser = parser;
    }

    public JsonParser getParser() {
        return fParser;
    }

    @Override
    public @Nullable ITmfLocation getLocation() {
        return new TmfLongLocation(getRank());
    }

    @Override
    public void dispose() {
        try {
            if (!fParser.isClosed()) {
                fParser.close();
            }
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        super.dispose();
    }

    public void skip(Long rank) throws JsonParseException, IOException {
        ChromiumEvent.skip(rank, fParser);
        setRank(rank);
    }

}
