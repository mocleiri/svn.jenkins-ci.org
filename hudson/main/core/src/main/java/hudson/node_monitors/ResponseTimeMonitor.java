package hudson.node_monitors;

import hudson.Util;
import hudson.model.Computer;
import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.util.TimeUnit2;
import hudson.util.IOException2;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Monitors the round-trip response time to this slave.
 *
 * @author Kohsuke Kawaguchi
 */
public class ResponseTimeMonitor extends NodeMonitor {
    public AbstractNodeMonitorDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final AbstractNodeMonitorDescriptor<Data> DESCRIPTOR = new AbstractNodeMonitorDescriptor<Data>() {
        protected Data monitor(Computer c) throws IOException, InterruptedException {
            Data old = get(c);
            Data d;

            long start = System.nanoTime();
            Future<String> f = c.getChannel().callAsync(new NoopTask());
            try {
                f.get(TIMEOUT, TimeUnit.MILLISECONDS);
                long end = System.nanoTime();
                d = new Data(old,TimeUnit2.NANOSECONDS.toMillis(end-start));
            } catch (ExecutionException e) {
                throw new IOException2(e.getCause());    // I don't think this is possible
            } catch (TimeoutException e) {
                // special constant to indicate that the processing timed out.
                d = new Data(old,-1L);
            }

            if(d.hasTooManyTimeouts()) {
                // TODO: this scheme should be generalized, so that Hudson can remember why it's marking the node
                // as offline, as well as allowing the user to force Hudson to use it.
                if(!c.isTemporarilyOffline()) {
                    LOGGER.warning(Messages.ResponseTimeMonitor_MarkedOffline(c.getName()));
                    c.setTemporarilyOffline(true);
                }
            }
            return d;
        }

        public String getDisplayName() {
            return Messages.ResponseTimeMonitor_DisplayName();
        }

        public NodeMonitor newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new ResponseTimeMonitor();
        }
    };

    /**
     * Immutable representation of the monitoring data.
     */
    @ExportedBean
    public static final class Data {
        /**
         * Record of the past 5 times. -1 if time out. Otherwise in milliseconds.
         * Old ones first.
         */
        private final long[] past5;

        private Data(Data old, long newDataPoint) {
            if(old==null)
                past5 = new long[] {newDataPoint};
            else {
                past5 = new long[Math.min(5,old.past5.length+1)];
                int copyLen = past5.length - 1;
                System.arraycopy(old.past5, old.past5.length-copyLen, this.past5, 0, copyLen);
                past5[past5.length-1] = newDataPoint;
            }
        }

        /**
         * Computes the recurrence of the time out
         */
        private int failureCount() {
            int cnt=0;
            for(int i=past5.length-1; i>=0 && past5[i]<0; i--, cnt++)
                ;
            return cnt;
        }

        /**
         * Computes the average response time, by taking the time out into account.
         */
        @Exported
        public long getAverage() {
            long total=0;
            for (long l : past5) {
                if(l<0)     total += TIMEOUT;
                else        total += l;
            }
            return total/past5.length;
        }

        public boolean hasTooManyTimeouts() {
            return failureCount()>=5;
        }

        /**
         * HTML rendering of the data
         */
        public String toString() {
//            StringBuilder buf = new StringBuilder();
//            for (long l : past5) {
//                if(buf.length()>0)  buf.append(',');
//                buf.append(l);
//            }
//            return buf.toString();
            int fc = failureCount();
            if(fc>0)
                return Util.wrapToErrorSpan(Messages.ResponseTimeMonitor_TimeOut(fc));
            return getAverage()+"ms";
        }
    }

    private static class NoopTask implements Callable<String,RuntimeException> {
        public String call() {
            return null;
        }

        private static final long serialVersionUID = 1L;
    }

    static {
        LIST.add(DESCRIPTOR);
    }

    /**
     * Time out interval in milliseconds.
     */
    private static final long TIMEOUT = 5000;

    private static final Logger LOGGER = Logger.getLogger(ResponseTimeMonitor.class.getName());
}
