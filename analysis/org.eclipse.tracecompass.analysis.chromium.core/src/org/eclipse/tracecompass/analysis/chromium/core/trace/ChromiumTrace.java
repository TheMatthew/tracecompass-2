package org.eclipse.tracecompass.analysis.chromium.core.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.Activator;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.internal.analysis.chromium.core.trace.ChromiumContext;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.json.JSONException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;

/**
 * @author matthew
 *
 */
public class ChromiumTrace extends TmfTrace implements ITmfPersistentlyIndexable, ITmfPropertiesProvider {

    private static final int ESTIMATED_EVENT_SIZE = 90;
    private static final TmfLongLocation NULL_LOCATION = new TmfLongLocation(-1L);
    private static final TmfContext INVALID_CONTEXT = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
    private static final TmfLongLocation INVALID_LOCATION = new TmfLongLocation(-1);

    private TmfLongLocation fCurrentLocation = INVALID_LOCATION;
    private File fFile;
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private final List<JsonParser> fParsers = new ArrayList<>();

    @Override
    public IStatus validate(IProject project, String path) {

        try (JsonParser parser = createParser(new File(path))) {
            ITmfEvent event = ChromiumEvent.parse(this, 0, parser);
            if (event == null) { // $NON-NLS-1$
                throw new JSONException("No events!"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(50, Activator.PLUGIN_ID);
        } catch (JSONException | IOException e) {
            return new TraceValidationStatus(0, Activator.PLUGIN_ID);
        }

    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fFile = new File(path);
        try (JsonParser parser = createParser(fFile)) {
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    private JsonParser createParser(File file) throws IOException, JsonParseException {
        JsonParser parser = JSON_FACTORY.createParser(file);
        parser.enable(Feature.ALLOW_COMMENTS);
        fParsers.add(parser);
        return parser;
    }

    @Override
    public synchronized void dispose() {
        fParsers.forEach(parser -> {
            try {
                if (!parser.isClosed()) {
                    parser.close();
                }
            } catch (IOException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
        });
        fParsers.clear();
        super.dispose();

    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fCurrentLocation;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        return (double) fCurrentLocation.getLocationInfo() / fFile.length();
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        if (fFile == null) {
            return INVALID_CONTEXT;
        }
        try {
            ChromiumContext context = null;
            Long rank = -1L;
            if (location == null || location.equals(NULL_LOCATION)) {
                context = new ChromiumContext(createParser(fFile));
                context.setLocation(new TmfLongLocation(0));
            } else if (location.getLocationInfo() instanceof Long) {
                context = new ChromiumContext(createParser(fFile));
                rank = (Long) location.getLocationInfo();
                context.skip(rank);
                context.setLocation(new TmfLongLocation(rank));
            }
            return context;
        } catch (final FileNotFoundException e) {
            Activator.getInstance().logError("Error seeking event. File not found: " + getPath(), e); //$NON-NLS-1$
            return INVALID_CONTEXT;
        } catch (final IOException e) {
            Activator.getInstance().logError("Error seeking event. File: " + getPath(), e); //$NON-NLS-1$
            return INVALID_CONTEXT;
        }
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        File file = fFile;
        if (file == null) {
            return INVALID_CONTEXT;
        }
        long filePos = (long) (file.length() * ratio);
        long estimatedRank = filePos / ESTIMATED_EVENT_SIZE;
        return seekEvent(new TmfLongLocation(estimatedRank));
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        if (context instanceof ChromiumContext) {
            ChromiumContext chromiumContext = (ChromiumContext) context;
            @Nullable
            ITmfLocation location = chromiumContext.getLocation();
            if (location instanceof TmfLongLocation) {
                TmfLongLocation tmfLongLocation = (TmfLongLocation) location;
                Long locationInfo = tmfLongLocation.getLocationInfo();
                if(location.equals(NULL_LOCATION)){
                    locationInfo = 0L;
                }
                if (locationInfo != null) {
                    try {
                        return ChromiumEvent.parse(this, locationInfo, chromiumContext.getParser());
                    } catch (IOException e) {
                        Activator.getInstance().logError("Error parsing event", e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        return Collections.singletonMap("Type", "Chromium");
    }

    @Override
    public ITmfLocation restoreLocation(ByteBuffer bufferIn) {
        return new TmfLongLocation(bufferIn);
    }



    @Override
    public int getCheckpointSize() {
        return 1000;
    }

}
