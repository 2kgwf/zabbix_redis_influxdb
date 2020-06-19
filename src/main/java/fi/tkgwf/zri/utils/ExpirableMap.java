package fi.tkgwf.zri.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpirableMap<K, V> implements Map<K, V> {

    protected final Map<K, ExpirableValue<V>> map = new HashMap<>();
    protected long recalcCounter = 0;

    @Override
    public int size() {
        cleanExpired();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        cleanExpired();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        cleanExpired();
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        ExpirableValue<V> get = map.get(key);
        return get != null && !get.isExpired() ? get.value : null;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("Putting without expiry timestamp not supported.");
    }

    public V put(K key, V value, long expiryTimestamp) {
        if (recalcCounter++ > 10000) {
            recalcCounter = 0;
            cleanExpired();
        }
        ExpirableValue<V> old = map.put(key, new ExpirableValue(value, expiryTimestamp));
        return old != null && !old.isExpired() ? old.value : null;
    }

    @Override
    public V remove(Object key) {
        ExpirableValue<V> old = map.remove(key);
        return old != null && !old.isExpired() ? old.value : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Putting without expiry timestamp not supported.");
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        cleanExpired();
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        cleanExpired();
        return map.values().stream().map(e -> e.value).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        cleanExpired();
        return map.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), e.getValue().value)).collect(Collectors.toSet());
    }

    protected void cleanExpired() {
        List<K> expired = map.entrySet().stream().filter(e -> e.getValue().isExpired()).map(Entry::getKey).collect(Collectors.toList());
        expired.forEach(map::remove);
    }

    class ExpirableValue<V> {

        protected final V value;
        protected final long expiryTimestamp;

        public ExpirableValue(V value, long expiryTimestamp) {
            this.value = value;
            this.expiryTimestamp = expiryTimestamp;
        }

        public boolean isExpired() {
            return expiryTimestamp < System.currentTimeMillis();
        }
    }
}
