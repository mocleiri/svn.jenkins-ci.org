/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.console;

import hudson.CloseProofOutputStream;
import hudson.MarkupText;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.remoting.ObjectInputStreamEx;
import hudson.util.FlushProofOutputStream;
import hudson.util.UnbufferedBase64InputStream;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 *
 * @param <T>
 *      Contextual model object that this console is associated with, such as {@link Run}.
 *
 * @author Kohsuke Kawaguchi
 * @see ConsoleAnnotationDescriptor
 */
public abstract class ConsoleAnnotation<T> implements Serializable, Describable<ConsoleAnnotation<?>> {
    /**
     * When the line of a console output that this annotation is attached is read by someone,
     * a new {@link ConsoleAnnotation} is de-serialized and this method is invoked to annotate that line.
     *
     * @param context
     *      The object that owns the console output in question.
     * @param text
     *      Represents a line of the console output being annotated.
     * @param charPos
     *      The character position in 'text' where this annotation is attached.
     *
     * @return
     *      if non-null value is returned, this annotator will handle the next line.
     *      this mechanism can be used to annotate multiple lines starting at the annotated position. 
     */
    public abstract ConsoleAnnotator annotate(T context, MarkupText text, int charPos);

    public ConsoleAnnotationDescriptor getDescriptor() {
        return (ConsoleAnnotationDescriptor)Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    public void encodeTo(OutputStream out) throws IOException {
        out.write(PREAMBLE);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(buf));
        oos.writeObject(this);
        oos.close();

        DataOutputStream dos = new DataOutputStream(new Base64OutputStream(new FlushProofOutputStream(new CloseProofOutputStream(out)),true,-1,null));
        // we don't need the size by ourselves, but it's useful to gracefully recover from an error
        // if the deserialization fail in the middle.
        dos.writeInt(buf.size());
        buf.writeTo(dos);
        dos.close();

        out.write(POSTAMBLE);
    }

    public static ConsoleAnnotation readFrom(DataInputStream in) throws IOException, ClassNotFoundException {
        byte[] preamble = new byte[PREAMBLE.length];
        in.readFully(preamble);
        if (!Arrays.equals(preamble,PREAMBLE))
            return null;    // not a valid preamble

        DataInputStream decoded = new DataInputStream(new UnbufferedBase64InputStream(in));
        int sz = decoded.readInt();
        byte[] buf = new byte[sz];
        decoded.readFully(buf);

        byte[] postamble = new byte[POSTAMBLE.length];
        in.readFully(postamble);
        if (!Arrays.equals(postamble,POSTAMBLE))
            return null;    // not a valid postamble

        ObjectInputStream ois = new ObjectInputStreamEx(
                new GZIPInputStream(new ByteArrayInputStream(buf)), Hudson.getInstance().pluginManager.uberClassLoader);
        return (ConsoleAnnotation) ois.readObject();
    }

    private static final long serialVersionUID = 1L;

    public static final byte[] PREAMBLE = "\u001B[8mha:".getBytes();
    public static final byte[] POSTAMBLE = "\u001B[0m".getBytes();

    /**
     * Locates the preamble in the given buffer.
     */
    public static int findPreamble(byte[] buf, int start, int len) {
        int e = start + len - PREAMBLE.length + 1;

        OUTER:
        for (int i=start; i<e; i++) {
            if (buf[i]==PREAMBLE[0]) {
                // check for the rest of the match
                for (int j=1; j<PREAMBLE.length; j++) {
                    if (buf[i+j]!=PREAMBLE[j])
                        continue OUTER;
                }
                return i; // found it
            }
        }
        return -1; // not found
    }
}
