package hudson.remoting;

import junit.framework.Test;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Test {@link Pipe}.
 *
 * @author Kohsuke Kawaguchi
 */
public class PipeTest extends RmiTestBase {
    /**
     * Test the "remote-write local-read" pipe.
     */
    public void testRemoteWrite() throws Exception {
        Pipe p = Pipe.createRemoteToLocal();
        Future<Integer> f = channel.callAsync(new WritingCallable(p));

        read(p);

        int r = f.get();
        System.out.println("result=" + r);
        assertEquals(5,r);
    }

    private static class WritingCallable implements Callable<Integer, IOException> {
        private final Pipe pipe;

        public WritingCallable(Pipe pipe) {
            this.pipe = pipe;
        }

        public Integer call() throws IOException {
            write(pipe);
            return 5;
        }
    }

    /**
     * Test the "local-write remote-read" pipe.
     */
    public void testLocalWrite() throws Exception {
        Pipe p = Pipe.createLocalToRemote();
        Future<Integer> f = channel.callAsync(new ReadingCallable(p));

        write(p);

        int r = f.get();
        System.out.println("result=" + r);
        assertEquals(5,r);
    }

    public void testLocalWrite2() throws Exception {
        Pipe p = Pipe.createLocalToRemote();
        Future<Integer> f = channel.callAsync(new ReadingCallable(p));

        Thread.sleep(2000); // wait for remote to connect to local.
        write(p);

        int r = f.get();
        System.out.println("result=" + r);
        assertEquals(5,r);
    }

    private static class ReadingCallable implements Callable<Integer, IOException> {
        private final Pipe pipe;

        public ReadingCallable(Pipe pipe) {
            this.pipe = pipe;
        }

        public Integer call() throws IOException {
            read(pipe);
            return 5;
        }

    }

    private static void write(Pipe pipe) throws IOException {
        OutputStream os = pipe.getOut();
        byte[] buf = new byte[384];
        for( int i=0; i<256; i++ ) {
            Arrays.fill(buf,(byte)i);
            os.write(buf,0,256);
        }
        os.close();
    }

    private static void read(Pipe p) throws IOException {
        InputStream in = p.getIn();
        for( int cnt=0; cnt<256*256; cnt++ )
            assertEquals(cnt/256,in.read());
        assertEquals(-1,in.read());
        in.close();
    }

    public static Test suite() throws Exception {
        return buildSuite(PipeTest.class);
    }
}
