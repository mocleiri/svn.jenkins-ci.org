/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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
package hudson.os.windows;

import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import jcifs.smb.SmbFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Windows slave installed/managed as a service entirely remotely
 *
 * @author Kohsuke Kawaguchi
 */
public class ManagedWindowsServiceLauncher extends ComputerLauncher {
    public void launch(SlaveComputer computer, StreamTaskListener listener) throws IOException, InterruptedException {
        String path = computer.getNode().getRemoteFS();
        // copy slave.jar
        SmbFile remoteRoot = new SmbFile("smb://" + computer.getName() + "/" + path.replace('\\', '/').replace(':', '$'));

        // copy exe
        copyAndClose(getClass().getResource("/windows-service/hudson.exe").openStream(),
                new SmbFile(remoteRoot,"hudson-slave.exe").getOutputStream());

        // copy slave.jar
        copyAndClose(Hudson.getInstance().getJnlpJars("slave.jar").getURL().openStream(),
                new SmbFile(remoteRoot,"hudson-slave.exe").getOutputStream());
        // TODO
        throw new UnsupportedOperationException();
    }

    private static void copyAndClose(InputStream in, OutputStream out) {
        try {
            IOUtils.copy(in,out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {
        public static final DescriptorImpl INSTANCE = new DescriptorImpl();

        public String getDisplayName() {
            return "Let Hudson control this Windows slave as a Windows service";
        }
    }
}
