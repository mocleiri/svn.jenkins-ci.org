package hudson.util;

import hudson.model.Fingerprint;
import hudson.model.FingerprintMap;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convenient base class for implementing data storage.
 *
 * <p>
 * One typical pattern of data storage in Hudson is the one that {@link Fingerprint}
 * uses, where each data is keyed by an unique key (MD5 sum), and that key is used
 * to determine the file system location of the data.
 *
 * On memory, each data is represented by one object ({@link Fingerprint}), and
 * write access to the same data is coordinated by using synchronization.
 *
 * <p>
 * With such storage, care has to be taken to ensure that there's only one data
 * object in memory for any given key. That means load and create operation
 * needs to be synchronized. This class implements this logic in a fairly efficient
 * way, and thus intends to help plugins that want to use such data storage.
 *
 * @since 1.87
 * @author Kohsuke Kawaguchi
 * @see FingerprintMap
 */
public abstract class KeyedDataStorage<T,P> {
    /**
     * The value is either {@code WeakReference<Fingerprint>} or {@link Loading}.
     *
     * If it's {@link WeakReference}, that represents the currently available value.
     * If it's {@link Loading}, then that indicates the fingerprint is being loaded.
     * The thread can wait on this object to be notified when the loading completes.
     */
    private final ConcurrentHashMap<String,Object> core = new ConcurrentHashMap<String,Object>();

    /**
     * Used in {@link KeyedDataStorage#core} to indicate that the loading of a fingerprint
     * is in progress, so that we can avoid creating two {@link Fingerprint}s for the same hash code,
     * but do so without having a single lock.
     */
    private static class Loading<T> {
        private T value;
        private boolean set;

        public synchronized void set(T value) {
            this.set = true;
            this.value = value;
            notifyAll();
        }

        /**
         * Blocks until the value is {@link #set(Object)} by another thread
         * and returns the value.
         */
        public synchronized T get() {
            try {
                while(!set)
                    wait();
                return value;
            } catch (InterruptedException e) {
                // assume the loading failed, but make sure we process interruption properly later
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    /**
     * Atomically gets the existing data object if any, or if it doesn't exist
     * {@link #create(String,Object) create} it and return it.
     *
     * @return
     *      Never null.
     * @param createParams
     *      Additional parameters needed to create a new data object. Can be null.
     */
    public T getOrCreate(String key, P createParams) throws IOException {
        return get(key,true,createParams);
    }

    /**
     * Finds the data object that matches the given key if available, or null
     * if not found.
     */
    public T get(String key) throws IOException {
        return get(key,false,null);
    }

    /**
     * Implementation of get/getOrCreate.
     */
    protected T get(String key, boolean createIfNotExist, P createParams) throws IOException {
        while(true) {
            Object value = core.get(key);

            if(value instanceof WeakReference) {
                WeakReference<T> wfp = (WeakReference<T>) value;
                T t = wfp.get();
                if(t!=null)
                    return t;  // found it
            }
            if(value instanceof Loading) {
                // another thread is loading it. get the value from there.
                T t = ((Loading<T>)value).get();
                if(t!=null || !createIfNotExist)
                    return t;   // found it (t!=null) or we are just 'get' (!createIfNotExist)
            }

            // the fingerprint doesn't seem to be loaded thus far, so let's load it now.
            // the care needs to be taken that other threads might be trying to do the same.
            Loading<T> l = new Loading<T>();
            if(value==null ? core.putIfAbsent(key,l)!=null : !core.replace(key,value,l)) {
                // the value has changed since then. another thread is attempting to do the same.
                // go back to square 1 and try it again.
                continue;
            }

            T t = null;
            try {
                t = load(key);
                if(t==null && createIfNotExist) {
                    t = create(key,createParams);    // create the new data
                    if(t==null)
                        throw new IllegalStateException(); // bug in the derived classes
                }
            } finally {
                // let other threads know that the value is available now.
                // when the original thread failed to load, this should set it to null.
                l.set(t);
            }

            // the map needs to be updated to reflect the result of loading
            if(t!=null)
                core.put(key,new WeakReference<T>(t));
            else
                core.remove(key);

            return t;
        }

    }

    /**
     * Attempts to load an existing data object from disk.
     *
     * <p>
     * {@link KeyedDataStorage} class serializes the requests so that
     * no two threads call the {@link #load(String)} method with the
     * same parameter concurrently. This ensures that there's only
     * up to one data object for any key.
     *
     * @return
     *      null if no such data exists.
     * @throws IOException
     *      if load operation fails. This exception will be
     *      propagated to the caller.
     */
    protected abstract T load(String key) throws IOException;

    /**
     * Creates a new data object.
     *
     * <p>
     * This method is called by {@link #getOrCreate(String,Object)}
     * if the data that matches the specified key does not exist.
     * <p>
     * Because of concurrency, another thread might call {@link #get(String)}
     * as soon as a new data object is created, so it's important that
     * this method returns a properly initialized "valid" object.
     *
     * @return
     *      never null. If construction fails, abort with an exception.
     * @throws IOException
     *      if the method fails to create a new data object, it can throw
     *      {@link IOException} (or any other exception) and that will be
     *      propagated to the caller.
     */
    protected abstract T create(String key, P createParams) throws IOException;

}
