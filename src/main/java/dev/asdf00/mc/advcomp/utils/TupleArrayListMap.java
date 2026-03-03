package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.mc.advcomp.types.RuntimeAssert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Conceptually implements a list of tuples of type K and V, where K may also be null and is otherwise easily mappable to V as in a hashmap
 *
 * @param <K> key (may be null for any number of elements; must be unique otherwise)
 * @param <V> value
 */
public class TupleArrayListMap<K, V> {
    // needs not be serialized
    private final HashMap<K, V> kvMapping;
    private final HashSet<V> nullElements;

    public TupleArrayListMap() {
        kvMapping = new HashMap<>();
        nullElements = new HashSet<>();
    }

    public void put(K key, V value) {
        if (key == null) {
            RuntimeAssert.RuntimeAssert(nullElements.add(value), "failed to add null-key as the value already existed!");
        } else {
            kvMapping.put(key, value);
        }
    }

    public V get(K key) {
        RuntimeAssert.RuntimeAssert(key != null, "key cannot be null");
        return kvMapping.get(key);
    }

    @SuppressWarnings("unchecked")
    public Tuple<K, V>[] entries() {
        var totalSize = kvMapping.size() + nullElements.size();
        var rvArray = new Tuple<?, ?>[totalSize];
        int currentDest = 0;
        for (var kv : kvMapping.entrySet()) {
            rvArray[currentDest++] = new Tuple<>(kv.getKey(), kv.getValue());
        }
        for (var val : nullElements) {
            rvArray[currentDest++] = new Tuple<>(null, val);
        }
        return (Tuple<K, V>[]) rvArray;
    }

    // -- serialization -- i.e. turn the map and set into two array lists and vice versa
    public record SerializeData<K, V>(ArrayList<K> a, ArrayList<V> b) {
    }

    @SuppressWarnings("unchecked")
    public SerializeData<K, V> getDataToSerialize() {
        var keysList = new ArrayList<K>();
        var valuesList = new ArrayList<V>();
        for (var kv : kvMapping.entrySet()) {
            keysList.add(kv.getKey());
            valuesList.add(kv.getValue());
        }

        for (var v : nullElements.toArray()) {
            keysList.add(null);
            valuesList.add((V) v);
        }

        return new SerializeData<>(keysList, valuesList);
    }

    @SuppressWarnings("unchecked")
    public TupleArrayListMap<K, V> fromUnserializedData(SerializeData<K, V> data) {
        var rv = new TupleArrayListMap<>();
        RuntimeAssert.RuntimeAssert(data.a.size() == data.b.size(), "unequal size");
        for (int i = 0; i < data.a.size(); i++) {
            rv.put(data.a.get(i), data.b.get(i));
        }
        return (TupleArrayListMap<K, V>) rv;
    }
}
