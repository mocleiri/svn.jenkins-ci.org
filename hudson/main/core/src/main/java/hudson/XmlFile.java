package hudson;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.XppReader;
import hudson.util.AtomicFileWriter;
import hudson.util.IOException2;
import hudson.util.XStream2;
import hudson.model.Descriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Represents an XML data file that Hudson uses as a data file.
 *
 *
 * <h2>Evolving data format</h2>
 * <p>
 * Changing data format requires a particular care so that users with
 * the old data format can migrate to the newer data format smoothly.
 *
 * <p>
 * Adding a field is the easiest. When you read an old XML that does
 * not have any data, the newly added field is left to the VM-default
 * value (if you let XStream create the object, such as
 * {@link #read()} &mdash; which is the majority), or to the value initialized by the
 * constructor (if the object is created via <tt>new</tt> and then its
 * value filled by XStream, such as {@link #unmarshal(Object)}.)
 *
 * <p>
 * Removing a field requires that you actually leave the field with
 * <tt>transient</tt> keyword. When you read the old XML, XStream
 * will set the value to this field. But when the data is saved,
 * the field will no longer will be written back to XML.
 * (It might be possible to tweak XStream so that we can simply
 * remove fields from the class. Any help appreciated.)
 *
 * <p>
 * Changing the data structure is usually a combination of the two
 * above. You'd leave the old data store with <tt>transient</tt>,
 * and then add the new data. When you are reading the old XML,
 * only the old field will be set. When you are reading the new XML,
 * only the new field will be set. You'll then need to alter the code
 * so that it will be able to correctly handle both situations,
 * and that as soon as you see data in the old field, you'll have to convert
 * that into the new data structure, so that the next <tt>save</tt> operation
 * will write the new data (otherwise you'll end up losing the data, because
 * old fields will be never written back.)
 *
 * <p>
 * In some limited cases (specifically when the class is the root object
 * to be read from XML, such as {@link Descriptor}), it is posible
 * to completely and drastically change the data format. See
 * {@link Descriptor#load()} for more about this technique.
 *
 * <p>
 * There's a few other possibilities, such as implementing a custom
 * {@link Converter} for XStream, or {@link XStream#alias(String, Class) registering an alias}.  
 *
 * @author Kohsuke Kawaguchi
 */
public final class XmlFile {
    private final XStream xs;
    private final File file;

    public XmlFile(File file) {
        this(DEFAULT_XSTREAM,file);
    }

    public XmlFile(XStream xs, File file) {
        this.xs = xs;
        this.file = file;
    }

    /**
     * Loads the contents of this file into a new object.
     */
    public Object read() throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            return xs.fromXML(r);
        } catch(StreamException e) {
            throw new IOException2("Unable to read "+file,e);
        } catch(ConversionException e) {
            throw new IOException2("Unable to read "+file,e);
        } finally {
            r.close();
        }
    }

    /**
     * Loads the contents of this file into an existing object.
     *
     * @return
     *      The unmarshalled object. Usually the same as <tt>o</tt>, but would be different
     *      if the XML representation if completely new.
     */
    public Object unmarshal( Object o ) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        try {
            return xs.unmarshal(new XppReader(r),o);
        } catch (StreamException e) {
            throw new IOException2(e);
        } catch(ConversionException e) {
            throw new IOException2("Unable to read "+file,e);
        } finally {
            r.close();
        }
    }

    public void write( Object o ) throws IOException {
        AtomicFileWriter w = new AtomicFileWriter(file);
        try {
            w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            xs.toXML(o,w);
            w.commit();
        } catch(StreamException e) {
            throw new IOException2(e);
        } finally {
            w.close();
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public void mkdirs() {
        file.getParentFile().mkdirs();
    }

    public String toString() {
        return file.toString();
    }

    /**
     * {@link XStream} instance is supposed to be thread-safe.
     */
    private static final XStream DEFAULT_XSTREAM = new XStream2();
}
