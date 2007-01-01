package hudson.model;

import hudson.util.CharSpool;
import hudson.util.CountingOutputStream;
import hudson.util.WriterOutputStream;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;

/**
 * Represents a large text data.
 *
 * <p>
 * This class defines methods for handling progressive text update.
 *
 * @author Kohsuke Kawaguchi
 */
public class LargeText {
    private final File file;
    private final boolean completed;

    public LargeText(File file, boolean completed) {
        this.file = file;
        this.completed = completed;
    }

    /**
     * Writes the tail portion of the file to the {@link Writer}.
     *
     * <p>
     * The text file is assumed to be in the system default encoding.
     *
     * @param start
     *      The byte offset in the input file where the write operation starts.
     *
     * @return
     *      if the file is still being written, this method writes the file
     *      until the last newline character and returns the offset to start
     *      the next write operation.
     */
    public long writeLogTo(long start, Writer w) throws IOException {
        CountingOutputStream os = new CountingOutputStream(new WriterOutputStream(w));

        RandomAccessFile f = new RandomAccessFile(file,"r");
        f.seek(start);

        if(completed) {
            // write everything till EOF
            byte[] buf = new byte[1024];
            int sz;
            while((sz=f.read(buf))>=0)
                os.write(buf,0,sz);
        } else {
            ByteBuf buf = new ByteBuf(null,f);
            HeadMark head = new HeadMark(buf);
            TailMark tail = new TailMark(buf);

            while(tail.moveToNextLine(f)) {
                head.moveTo(tail,os);
            }
            head.finish(os);
        }

        f.close();
        os.flush();

        return os.getCount()+start;
    }

    /**
     * Implements the progressive text handling.
     * This method is used as a "web method" with progressiveText.jelly.
     */
    public void doProgressText(StaplerRequest req, StaplerResponse rsp) throws IOException {
        rsp.setContentType("text/plain");
        rsp.setCharacterEncoding("UTF-8");
        rsp.setStatus(HttpServletResponse.SC_OK);

        if(!file.exists()) {
            // file doesn't exist yet
            rsp.addHeader("X-Text-Size","0");
            rsp.addHeader("X-More-Data","true");
            return;
        }

        long start = 0;
        String s = req.getParameter("start");
        if(s!=null)
            start = Long.parseLong(s);

        if(file.length() < start )
            start = 0;  // text rolled over

        CharSpool spool = new CharSpool();
        long r = writeLogTo(start,spool);

        rsp.addHeader("X-Text-Size",String.valueOf(r));
        if(!completed)
            rsp.addHeader("X-More-Data","true");

        spool.writeTo(rsp.getWriter());

    }

    /**
     * Points to a byte in the buffer.
     */
    private static class Mark {
        protected ByteBuf buf;
        protected int pos;

        public Mark(ByteBuf buf) {
            this.buf = buf;
        }
    }

    /**
     * Points to the start of the region that's not committed
     * to the output yet.
     */
    private static final class HeadMark extends Mark {
        public HeadMark(ByteBuf buf) {
            super(buf);
        }

        /**
         * Moves this mark to 'that' mark, and writes the data
         * to {@link OutputStream} if necessary.
         */
        void moveTo(Mark that, OutputStream os) throws IOException {
            while(this.buf!=that.buf) {
                os.write(buf.buf,0,buf.size);
                buf = buf.next;
                pos = 0;
            }

            this.pos = that.pos;
        }

        void finish(OutputStream os) throws IOException {
            os.write(buf.buf,0,pos);
        }
    }

    /**
     * Points to the end of the region.
     */
    private static final class TailMark extends Mark {
        public TailMark(ByteBuf buf) {
            super(buf);
        }

        boolean moveToNextLine(RandomAccessFile f) throws IOException {
            while(true) {
                while(pos==buf.size) {
                    if(!buf.isFull()) {
                        // read until EOF
                        return false;
                    } else {
                        // read into the next buffer
                        buf = new ByteBuf(buf,f);
                        pos = 0;
                    }
                }
                byte b = buf.buf[pos++];
                if(b=='\r' || b=='\n')
                    return true;
            }
        }
    }

    private static final class ByteBuf {
        private final byte[] buf = new byte[1024];
        private int size = 0;
        private ByteBuf next;

        public ByteBuf(ByteBuf previous, RandomAccessFile f) throws IOException {
            if(previous!=null) {
                assert previous.next==null;
                previous.next = this;
            }

            while(!this.isFull()) {
                int chunk = f.read(buf, size, buf.length - size);
                if(chunk==-1)
                    return;
                size+= chunk;
            }
        }

        public boolean isFull() {
            return buf.length==size;
        }
    }
}
