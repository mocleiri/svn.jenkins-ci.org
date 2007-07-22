package hudson.util;

import com.thoughtworks.xstream.alias.CannotResolveClassException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link List}-like implementation that has copy-on-write semantics.
 *
 * <p>
 * This class is suitable where highly concurrent access is needed, yet
 * the write operation is relatively uncommon.
 *
 * @author Kohsuke Kawaguchi
 */
public class CopyOnWriteList<E> implements Iterable<E> {
    private volatile List<? extends E> core;

    public CopyOnWriteList(List<E> core) {
        this(core,false);
    }

    private CopyOnWriteList(List<E> core, boolean noCopy) {
        this.core = noCopy ? core : new ArrayList<E>(core);
    }

    public CopyOnWriteList() {
        this.core = Collections.emptyList();
    }

    public synchronized void add(E e) {
        List<E> n = new ArrayList<E>(core);
        n.add(e);
        core = n;
    }

    /**
     * Removes an item from the list.
     *
     * @return
     *      true if the list contained the item. False if it didn't,
     *      in which case there's no change.
     */
    public synchronized boolean remove(E e) {
        List<E> n = new ArrayList<E>(core);
        boolean r = n.remove(e);
        core = n;
        return r;
    }

    /**
     * Returns an iterator.
     *
     * The returned iterator doesn't support the <tt>remove</tt> operation.
     */
    public Iterator<E> iterator() {
        final Iterator<? extends E> itr = core.iterator();
        return new Iterator<E>() {
            public boolean hasNext() {
                return itr.hasNext();
            }

            public E next() {
                return itr.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Completely replaces this list by the contents of the given list.
     */
    public void replaceBy(CopyOnWriteList<? extends E> that) {
        this.core = that.core;
    }

    /**
     * Completely replaces this list by the contents of the given list.
     */
    public void replaceBy(Collection<? extends E> that) {
        this.core = new ArrayList<E>(that);
    }

    public void clear() {
        this.core = new ArrayList<E>();
    }

    public E[] toArray(E[] array) {
        return core.toArray(array);
    }

    public void addAllTo(Collection<? super E> dst) {
        dst.addAll(core);
    }

    /**
     * {@link Converter} implementation for XStream.
     */
    public static final class ConverterImpl extends AbstractCollectionConverter {
        public ConverterImpl(Mapper mapper) {
            super(mapper);
        }

        public boolean canConvert(Class type) {
            return type==CopyOnWriteList.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            for (Object o : (CopyOnWriteList) source)
                writeItem(o, context, writer);
        }

        @SuppressWarnings("unchecked")
        public CopyOnWriteList unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            // read the items from xml into a list
            List items = new ArrayList();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                try {
                    Object item = readItem(reader, context, items);
                    items.add(item);
                } catch (CannotResolveClassException e) {
                    LOGGER.log(Level.WARNING,"Failed to resolve class",e);
                }
                reader.moveUp();
            }

            return new CopyOnWriteList(items,true);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CopyOnWriteList.class.getName());
}
