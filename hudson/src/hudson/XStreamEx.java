package hudson;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.XppReader;
import hudson.util.AtomicFileWriter;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;

/**
 * {@link XStream} with helper methods.
 *
 * @author Kohsuke Kawaguchi
 */
public class XStreamEx extends XStream {
    public Object fromXML( File f ) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            return fromXML(r);
        } catch(StreamException e) {
            throw new IOException2(e);
        } finally {
            r.close();
        }
    }

    public void unmarshal( File f, Object o ) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
        try {
            unmarshal(new XppReader(r),o);
        } catch (StreamException e) {
            throw new IOException2(e);
        } finally {
            r.close();
        }
    }

    public void toXML( Object o, File f ) throws IOException {
        AtomicFileWriter w = new AtomicFileWriter(f);
        try {
            w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            toXML(o,w);
            w.commit();
        } catch(StreamException e) {
            throw new IOException2(e);
        } finally {
            w.close();
        }
    }
}
