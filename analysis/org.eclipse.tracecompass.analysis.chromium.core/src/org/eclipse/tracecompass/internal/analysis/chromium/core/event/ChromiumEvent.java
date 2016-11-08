package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.io.IOException;

import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumFields.Phase;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class ChromiumEvent extends TmfEvent {

    public ChromiumEvent(ITmfTrace trace, long rank, ITmfTimestamp ts, ITmfEventType type, ChromiumFields content) {
        super(trace, rank, ts, type, content);
    }

    public static void skip(long number, JsonParser jsonParser) throws JsonParseException, IOException {
        for (long i = 0; i < number; i++) {
            JsonToken token = jsonParser.nextToken();
            while (!token.equals(JsonToken.END_OBJECT)) {
                token = jsonParser.nextToken();
            }
        }
    }

    public static ITmfEvent parse(ITmfTrace trace, long rank, JsonParser jsonParser)
            throws JsonParseException, IOException {
        int tid = -1;
        String pid = null;
        ITmfTimestamp ts = null;
        long duration = -1;
        char ph = '-';
        String name = null;
        String cat = null;
        int id = -1;
        JsonToken token = jsonParser.nextToken();
        if (token == null) {
            return null;
        }
        while (!token.equals(JsonToken.END_OBJECT)) {
            if (token.equals(JsonToken.FIELD_NAME)) {
                String fieldName = jsonParser.getCurrentName();
                token = jsonParser.nextValue();
                switch (fieldName) {
                case "dur":
                    duration = parseTs(jsonParser.getText());
                    break;
                case "ts":
                    ts = TmfTimestamp.fromNanos(parseTs(jsonParser.getText()));
                    break;
                case "tid":
                    tid = jsonParser.getValueAsInt(-1);
                    break;
                case "pid":
                    pid = jsonParser.getValueAsString();
                    break;
                case "ph": // phase
                    ph = jsonParser.getValueAsString().charAt(0);
                    break;
                case "name":
                    name = jsonParser.getValueAsString();
                    break;
                case "cat":
                    name = jsonParser.getValueAsString();
                    break;
                case "id":
                    id = jsonParser.getValueAsInt(-1);
                    break;
                default:
                    throw new IllegalStateException(fieldName);
                }
            }
            token = jsonParser.nextToken();
            if (token == null) {
                return null;
            }
        }
        return new ChromiumEvent(trace, rank, ts, ChromiumType.get(name),
                new ChromiumFields(name, cat, tid, pid, ph, id, duration));
    }

    private static long parseTs(String ts) {
        long valInNs = 0;
        int length = ts.length();
        for (int i = 0; i < length; i++) {
            int val = CHAR_ARRAY[ts.charAt(i)];
            if (val == Integer.MIN_VALUE) {
                int countDown = 3;
                while ((i + 1) < length && countDown > 0) {
                    countDown--;
                    i++;
                    val = CHAR_ARRAY[ts.charAt(i)];
                    valInNs *= 10;
                    valInNs += val;
                }
                return valInNs;
            }
            valInNs *= 10;
            valInNs += val;
        }
        return valInNs;
    }

    private static final int CHAR_ARRAY[] = new int[256];
    static {
        CHAR_ARRAY['0'] = 0;
        CHAR_ARRAY['1'] = 1;
        CHAR_ARRAY['2'] = 2;
        CHAR_ARRAY['3'] = 3;
        CHAR_ARRAY['4'] = 4;
        CHAR_ARRAY['5'] = 5;
        CHAR_ARRAY['6'] = 6;
        CHAR_ARRAY['7'] = 7;
        CHAR_ARRAY['8'] = 8;
        CHAR_ARRAY['9'] = 9;
        CHAR_ARRAY['.'] = Integer.MIN_VALUE;
    }

    public Phase getPhase() {
        return ((ChromiumFields)getContent()).getPh();
    }
}