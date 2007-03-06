package hudson.remoting;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Request/response pattern over {@link Channel}, the layer-1 service.
 *
 * <p>
 * This assumes that the receiving side has all the class definitions
 * available to de-serialize {@link Request}, just like {@link Command}.
 *
 * @author Kohsuke Kawaguchi
 * @see Response
 */
abstract class Request<RSP extends Serializable,EXC extends Throwable> extends Command {

    /**
     * Executed on a remote system to perform the task.
     *
     * @param channel
     *      The local channel. From the view point of the JVM that
     *      {@link #call(Channel) made the call}, this channel is
     *      the remote channel.
     * @return
     *      the return value will be sent back to the calling process.
     * @throws EXC
     *      The exception will be forwarded to the calling process.
     *      If no checked exception is supposed to be thrown, use {@link RuntimeException}.
     */
    protected abstract RSP perform(Channel channel) throws EXC;

    /**
     * Uniquely identifies this request.
     * Used for correlation between request and response.
     */
    private final int id;

    private volatile Response<RSP,EXC> response;

    protected Request() {
        synchronized(Request.class) {
            id = nextId++;
        }
    }

    /**
     * Sends this request to a remote system, and blocks until we receives a response.
     *
     * @param channel
     *      The channel from which the request will be sent.
     * @throws InterruptedException
     *      If the thread is interrupted while it's waiting for the call to complete.
     * @throws IOException
     *      If there's an error during the communication.
     * @throws RequestAbortedException
     *      If the channel is terminated while the call is in progress.
     * @throws EXC
     *      If the {@link #perform(Channel)} throws an exception.
     */
    public final RSP call(Channel channel) throws EXC, InterruptedException, IOException {
        // Channel.send() locks channel, and there are other call sequences
        // (  like Channel.terminate()->Request.abort()->Request.onCompleted()  )
        // that locks channel -> request, so lock objects in the same order
        synchronized(channel) {
            synchronized(this) {
                response=null;

                channel.pendingCalls.put(id,this);
                channel.send(this);
            }
        }

        synchronized(this) {
            while(response==null)
                wait(); // wait until the response arrives

            Object exc = response.exception;

            if(exc !=null)
                throw (EXC)exc; // some versions of JDK fails to compile this line. If so, upgrade your JDK.

            return response.returnValue;
        }
    }

    /**
     * Makes an invocation but immediately returns without waiting for the completion
     * (AKA asynchronous invocation.)
     *
     * @param channel
     *      The channel from which the request will be sent.
     * @return
     *      The {@link Future} object that can be used to wait for the completion.
     * @throws IOException
     *      If there's an error during the communication.
     */
    public final Future<RSP> callAsync(Channel channel) throws IOException {
        response=null;

        channel.pendingCalls.put(id,this);
        channel.send(this);

        return new Future<RSP>() {
            /**
             * The task cannot be cancelled.
             */
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            public boolean isCancelled() {
                return false;
            }

            public boolean isDone() {
                return response!=null;
            }

            public RSP get() throws InterruptedException, ExecutionException {
                synchronized(Request.this) {
                    while(response==null)
                        Request.this.wait(); // wait until the response arrives

                    if(response.exception!=null)
                        throw new ExecutionException(response.exception);

                    return response.returnValue;
                }
            }

            public RSP get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                synchronized(Request.this) {
                    if(response==null)
                        Request.this.wait(unit.toMillis(timeout)); // wait until the response arrives
                    if(response==null)
                        throw new TimeoutException();

                    if(response.exception!=null)
                        throw new ExecutionException(response.exception);

                    return response.returnValue;
                }
            }
        };
    }


    /**
     * Called by the {@link Response} when we received it.
     */
    /*package*/ synchronized void onCompleted(Response<RSP,EXC> response) {
        this.response = response;
        notify();
    }

    /**
     * Aborts the processing. The calling thread will receive an exception. 
     */
    /*package*/ void abort(IOException e) {
        onCompleted(new Response(id,new RequestAbortedException(e)));
    }

    /**
     * Schedules the execution of this request.
     */
    protected final void execute(final Channel channel) {
        channel.executor.execute(new Runnable() {
            public void run() {
                try {
                    RSP rsp;
                    try {
                        rsp = Request.this.perform(channel);
                    } catch (Throwable t) {
                        // error return
                        channel.send(new Response<RSP,Throwable>(id,t));
                        return;
                    }
                    // normal completion
                    channel.send(new Response<RSP,EXC>(id,rsp));
                } catch (IOException e) {
                    // communication error.
                    // this means the caller will block forever
                    logger.log(Level.SEVERE, "Failed to send back a reply",e);
                }
            }
        });
    }

    /**
     * Next request ID.
     */
    private static int nextId=0;

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Request.class.getName());

    //private static final Unsafe unsafe = getUnsafe();

    //private static Unsafe getUnsafe() {
    //    try {
    //        Field f = Unsafe.class.getDeclaredField("theUnsafe");
    //        f.setAccessible(true);
    //        return (Unsafe)f.get(null);
    //    } catch (NoSuchFieldException e) {
    //        throw new Error(e);
    //    } catch (IllegalAccessException e) {
    //        throw new Error(e);
    //    }
    //}
}
