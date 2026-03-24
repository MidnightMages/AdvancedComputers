package dev.asdf00.mc.advcomp.types.cluster;

import java.util.concurrent.atomic.AtomicInteger;

public final class ClusterType {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    private final int id;
    private final String clusterName;

    public ClusterType(String clusterName) {
        this.id = nextId.getAndIncrement();
        this.clusterName = clusterName;
    }

    public int getId() {
        return id;
    }

    public String getClusterName() {
        return clusterName;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ClusterType act) && (act.id == this.id);
    }
}
