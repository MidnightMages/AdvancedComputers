package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.types.cluster.ClusterType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClusterTypeManager {
    private static final ClusterTypeManager SINGLETON = new ClusterTypeManager();

    private final HashMap<String, ClusterType> clusterTypesByName = new HashMap<>();
    private boolean isClosed = false;
    private final Object lockObj = new Object();


    public ClusterType registerNewClusterType(String name) {
        synchronized (lockObj) {
            if (isClosed)
                throw new IllegalStateException("Attempted to register a network type too late!");

            if (clusterTypesByName.containsKey(name))
                throw new IllegalStateException("Network type '%s' has already been registered!".formatted(name));

            var n = new ClusterType(name);
            clusterTypesByName.put(name, n);
            return n;
        }
    }

    /**
     * SHALL ONLY BE CALLED BY ADVANCED COMPUTERS
     */
    public void closeRegistration() {
        isClosed = true;
    }

    public Map<String, ClusterType> getNetworkTypes() {
        return Collections.unmodifiableMap(clusterTypesByName);
    }

    
    public static ClusterTypeManager getInstance() {
        return SINGLETON;
    }
}
