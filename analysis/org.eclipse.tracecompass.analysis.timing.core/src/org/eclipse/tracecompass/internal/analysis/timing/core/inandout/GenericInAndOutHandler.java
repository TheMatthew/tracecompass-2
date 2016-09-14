package org.eclipse.tracecompass.internal.analysis.timing.core.inandout;

public interface GenericInAndOutHandler {

    public class Begin extends AbstractInAndOutBeginHandler{

        private String fKey;

        Begin(String key){
            fKey = key;
        }

        @Override
        protected String getKey() {
            return fKey;
        }

    }

    public class End extends AbstractInAndOutEndHandler{

        private String fKey;

        End(String key){
            fKey = key;
        }

        @Override
        protected String getKey() {
            return fKey;
        }

    }

}
