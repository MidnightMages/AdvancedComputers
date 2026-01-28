package dev.asdf00.mc.advcomp.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a bidirectional map, mapping 1 key to N values.
 */
public class SetBiMap<K, V> {
    private final HashMap<K, V> forward;
    private final HashMap<V, Set<K>> backward;

    public SetBiMap() {
        forward = new HashMap<>();
        backward = new HashMap<>();
    }

    public void put(K key, V value) {
        dropBackRef(key);
        forward.put(key, value);
        backward.computeIfAbsent(value, k -> new HashSet<>()).add(key);
    }

    public void remove(K key) {
        dropBackRef(key);
        forward.remove(key);
    }

    public V get(K key) {
        return forward.get(key);
    }

    public Set<K> getBack(V value) {
        return backward.getOrDefault(value, Set.of());
    }

    private void dropBackRef(K key) {
        V prevV = forward.get(key);
        if (prevV != null) {
            Set<K> mySet = backward.get(prevV);
            if (mySet != null) {
                mySet.remove(key);
                if (mySet.isEmpty()) {
                    backward.remove(prevV);
                }
            }
        }
    }
}
