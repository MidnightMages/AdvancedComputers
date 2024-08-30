package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AcClusterTypeManager {
    private final HashMap<String, AcClusterType> clusterTypesByName = new HashMap<>();
    private boolean isClosed = false;

    public void closeRegistration() {
        isClosed = true;
    }

    private final Object lockObj = new Object();

    public AcClusterType RegisterNewClusterType(String name) {
        synchronized (lockObj) {
            if (isClosed)
                throw new IllegalStateException("Attempted to register a network type too late!");

            if (clusterTypesByName.containsKey(name))
                throw new IllegalStateException("Network type '%s' has already been registered!".formatted(name));

            var n = new AcClusterType(name);
            clusterTypesByName.put(name, n);
            return n;
        }
    }

    public Map<String, AcClusterType> GetNetworkTypes() {
        return Collections.unmodifiableMap(clusterTypesByName);
    }

    private static final AcClusterTypeManager SINGLETON = new AcClusterTypeManager();
    
    public static AcClusterTypeManager getInstance() {
        return SINGLETON;
    }
}
