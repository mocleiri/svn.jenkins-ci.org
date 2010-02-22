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

import com.trilead.ssh2.crypto.Base64;
import hudson.model.Hudson;
import hudson.remoting.ObjectInputStreamEx;
import hudson.util.IOException2;
import hudson.util.TimeUnit2;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.Math.abs;

/**
 *
 * @param <T>
 *      Context type.
 * @author Kohsuke Kawaguchi
 */
public class AnnotatedLargeText<T> extends LargeText {
    private final File logFile;
    private T context;

    public AnnotatedLargeText(File file, Charset charset, boolean completed, T context) {
        super(file, charset, completed);
        this.logFile = file;
        this.context = context;
    }

    private ConsoleAnnotator createAnnotator(StaplerRequest req) throws IOException {
        try {
            String base64 = req.getHeader("X-ConsoleAnnotator");
            if (base64!=null) {
                Cipher sym = Cipher.getInstance("AES");
                sym.init(Cipher.DECRYPT_MODE, Hudson.getInstance().getSecretKeyAsAES128());

                ObjectInputStream ois = new ObjectInputStreamEx(new GZIPInputStream(
                        new CipherInputStream(new ByteArrayInputStream(Base64.decode(base64.toCharArray())),sym)),
                        Hudson.getInstance().pluginManager.uberClassLoader);
                long timestamp = ois.readLong();
                if (TimeUnit2.HOURS.toMillis(1) > abs(System.currentTimeMillis()-timestamp))
                    // don't deserialize something too old to prevent a replay attack
                    return (ConsoleAnnotator)ois.readObject();
            }
        } catch (GeneralSecurityException e) {
            throw new IOException2(e);
        } catch (ClassNotFoundException e) {
            throw new IOException2(e);
        }
        // start from scratch
        return ConsoleAnnotator.initial(context.getClass());
    }

    @Override
    public long writeLogTo(long start, Writer w) throws IOException {
        ConsoleAnnotationOutputStream caw = new ConsoleAnnotationOutputStream(
                w, createAnnotator(Stapler.getCurrentRequest()), context, charset);
        long r = super.writeLogTo(start,caw);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Cipher sym = Cipher.getInstance("AES");
            sym.init(Cipher.ENCRYPT_MODE,Hudson.getInstance().getSecretKeyAsAES128());
            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new CipherOutputStream(baos,sym)));
            oos.writeLong(System.currentTimeMillis()); // send timestamp to prevent a replay attack
            oos.writeObject(caw.getConsoleAnnotator());
            oos.close();
            Stapler.getCurrentResponse().setHeader("X-ConsoleAnnotator",new String(Base64.encode(baos.toByteArray())));
        } catch (GeneralSecurityException e) {
            throw new IOException2(e);
        }
        return r;
    }

}
