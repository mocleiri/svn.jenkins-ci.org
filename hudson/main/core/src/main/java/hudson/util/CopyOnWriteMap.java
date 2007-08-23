package hudson.util;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.collections.TreeMapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@link Map} that has copy-on-write semantics.
 *
 * <p>
 * This class is suitable where highly concurrent access is needed, yet
 * the write operation is relatively uncommon.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CopyOnWriteMap<K,V> implements Map<K,V> {
    protected volatile Map<K,V> core;
    /**
     * Read-only view of {@link #core}.
     */
    private volatile Map<K,V> view;

    protected CopyOnWriteMap(Map<K, V> core) {
        update(core);
    }

    protected CopyOnWriteMap() {
        update(Collections.<K,V>emptyMap());
    }

    private void update(Map<K, V> m) {
        core = m;
        view = Collections.unmodifiableMap(core);
    }

    public int size() {
        return core.size();
    }

    public boolean isEmpty() {
        return core.isEmpty();
    }

    public boolean containsKey(Object key) {
        return core.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return core.containsValue(value);
    }

    public V get(Object key) {
        return core.get(key);
    }

    public synchronized V put(K key, V value) {
        Map<K,V> m = copy();
        V r = m.put(key,value);
        update(m);

        return r;
    }

    public synchronized V remove(Object key) {
        Map<K,V> m = copy();
        V r = m.remove(key);
        update(m);

        return r;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> t) {
        Map<K,V> m = copy();
        m.putAll(t);
        update(m);
    }

    protected abstract Map<K,V> copy();

    public synchronized void clear() {
        update(Collections.<K,V>emptyMap());
    }

    /**
     * This method will return a read-only {@link Set}.
     */
    public Set<K> keySet() {
        return view.keySet();
    }

    /**
     * This method will return a read-only {@link Collection}.
     */
    public Collection<V> values() {
        return view.values();
    }

    /**
     * This method will return a read-only {@link Set}.
     */
    public Set<Entry<K,V>> entrySet() {
        return view.entrySet();
    }

    /**
     * {@link CopyOnWriteMap} backed by {@link HashMap}.
     */
    public static final class Hash<K,V> extends CopyOnWriteMap<K,V> {
        public Hash(Map<K, V> core) {
            super(new LinkedHashMap<K,V>(core));
        }

        public Hash() {
        }

        protected Map<K,V> copy() {
            return new LinkedHashMap<K,V>(core);
        }

        public static class ConverterImpl extends MapConverter {
            public ConverterImpl(Mapper mapper) {
                super(mapper);
            }

            @Override
            public boolean canConvert(Class type) {
                return type==Hash.class;
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                return new Hash((Map) super.unmarshal(reader,context));
            }

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                super.marshal(((Hash)source).core,writer,context);
            }
        }
    }

    /**
     * {@link CopyOnWriteMap} backed by {@link TreeMap}.
     */
    public static final class Tree<K,V> extends CopyOnWriteMap<K,V> {
        private final Comparator<K> comparator;

        public Tree(Map<K,V> core, Comparator<K> comparator) {
            this(comparator);
            putAll(core);
        }

        public Tree(Comparator<K> comparator) {
            this.comparator = comparator;
        }

        public Tree() {
            this(null);
        }

        protected Map<K,V> copy() {
            TreeMap<K, V> m = new TreeMap<K, V>(comparator);
            m.putAll(core);
            return m;
        }

        public static class ConverterImpl extends TreeMapConverter {
            public ConverterImpl(Mapper mapper) {
                super(mapper);
            }

            @Override
            public boolean canConvert(Class type) {
                return type==Tree.class;
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                TreeMap tm = (TreeMap) super.unmarshal(reader,context);
                return new Tree(tm,tm.comparator());
            }

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                super.marshal(((Tree)source).core,writer,context);
            }
        }
    }
}
