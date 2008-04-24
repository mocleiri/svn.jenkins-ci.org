package hudson.model;

import hudson.util.ByteBuffer;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;

/**
 * {@link Thread} for performing one-off task.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.191
 */
public abstract class TaskThread extends Thread {
    /**
     * Represents the output from this task thread.
     */
    private final LargeText text;

    /**
     * Represents the interface to produce output.
     */
    private TaskListener listener;

    private final TaskAction owner;

    /**
     *
     * @param output
     *      Determines where the output from this task thread goes.
     */
    protected TaskThread(TaskAction owner, ListenerAndText output) {
        //FIXME this failed to compile super(owner.getBuild().toString()+' '+owner.getDisplayName());
        //Please implement more general way how to get information about action owner, 
        //if you want it in the thread's name.
        super(owner.getDisplayName());
        this.owner = owner;
        this.text = output.text;
        this.listener = output.listener;
    }

    public Reader readAll() throws IOException {
        // this method can be invoked from another thread.
        return text.readAll();
    }

    /**
     * Registers that this {@link TaskThread} is run for the specified
     * {@link TaskAction}. This can be explicitly called from subtypes
     * to associate a single {@link TaskThread} across multiple tag actions.
     */
    protected final void associateWith(TaskAction action) {
        action.workerThread = this;
        action.log = new WeakReference<LargeText>(text);
    }

    public synchronized void start() {
        associateWith(owner);
        super.start();
    }

    /**
     * Determines where the output of this {@link TaskThread} goes.
     * <p>
     * Subclass can override this to send the output to a file, for example.
     */
    protected ListenerAndText createListener() throws IOException {
        return ListenerAndText.forMemory();
    }

    public final void run() {
        try {
            perform(listener);
            listener.getLogger().println("Completed");
            owner.workerThread = null;            
        } catch (InterruptedException e) {
            listener.getLogger().println("Aborted");
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        } finally {
            listener = null;
        }
        text.markAsComplete();
    }

    /**
     * Do the actual work.
     *
     * @throws Exception
     *      The exception is recorded and reported as a failure.
     */
    protected abstract void perform(TaskListener listener) throws Exception;

    /**
     * Tuple of {@link TaskListener} and {@link LargeText}, representing
     * the interface for producing output and how to retrieve it later.
     */
    public static final class ListenerAndText {
        final TaskListener listener;
        final LargeText text;

        public ListenerAndText(TaskListener listener, LargeText text) {
            this.listener = listener;
            this.text = text;
        }

        /**
         * Creates one that's backed by memory.
         */
        public static ListenerAndText forMemory() {
            // StringWriter is synchronized
            ByteBuffer log = new ByteBuffer();

            return new ListenerAndText(
                new StreamTaskListener(log),
                new LargeText(log,false)
            );
        }

        /**
         * Creates one that's backed by a file. 
         */
        public static ListenerAndText forFile(File f) throws IOException {
            return new ListenerAndText(
                new StreamTaskListener(f),
                new LargeText(f,false)
            );
        }
    }
}
