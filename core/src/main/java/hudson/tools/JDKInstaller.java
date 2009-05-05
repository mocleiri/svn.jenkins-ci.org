/*
 * The MIT License
 *
 * Copyright (c) 2009, Sun Microsystems, Inc.
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
package hudson.tools;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.FilePath.FileCallable;
import hudson.model.Node;
import hudson.model.TaskListener;
import static hudson.tools.JDKInstaller.Preference.*;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Install JDKs from java.sun.com.
 *
 * @author Kohsuke Kawaguchi
 */
public class JDKInstaller extends ToolInstaller {
    /**
     * The release ID that Sun assigns to each JDK, such as "jdk-6u13-oth-JPR@CDS-CDS_Developer"
     *
     * <p>
     * This ID can be seen in the "ProductRef" query parameter of the download page, like
     * https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef=jdk-6u13-oth-JPR@CDS-CDS_Developer
     */
    public final String id;

    /**
     * We require that the user accepts the license by clicking a checkbox, to make up for the part
     * that we auto-accept cds.sun.com license click through.
     */
    public final boolean acceptLicense;

    @DataBoundConstructor
    public JDKInstaller(String id, boolean acceptLicense) {
        super(null);
        this.id = id;
        this.acceptLicense = acceptLicense;
    }

    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expectedLocation = node.createPath(tool.getHome());
        PrintStream out = log.getLogger();
        try {
            if(!acceptLicense) {
                out.println("Unable to perform installation until the license is accepted.");
                return expectedLocation;
            }
            // already installed?
            FilePath marker = expectedLocation.child(".installedByHudson");
            if(marker.exists())
                return expectedLocation;
            expectedLocation.mkdirs();

            Platform p = Platform.of(node);
            URL url = locate(log, p, CPU.of(node));

            out.println("Downloading "+url);
            FilePath file = expectedLocation.child(fileName(p));
            file.copyFrom(url);

            out.println("Installing "+file);
            switch (p) {
            case LINUX:
            case SOLARIS:
                file.chmod(0755);
                if(node.createLauncher(log).launch(new String[]{file.getRemote(),"-noregister"},new String[0],new ByteArrayInputStream("yes".getBytes()),out,expectedLocation).join()!=0)
                    throw new AbortException("Failed to install JDK");

                // JDK creates its own sub-directory, so pull them up
                List<FilePath> paths = expectedLocation.list(JDK_FINDER);
                if(paths.size()!=1)
                    throw new AbortException("Failed to find the extracted JDKs: "+paths);

                paths.get(0).act(PULLUP_DIRECTORY);

                // clean up
                paths.get(0).delete();
                file.delete();

                marker.touch(System.currentTimeMillis());
                break;
            case WINDOWS:
                // TODO: implement this later
                throw new UnsupportedOperationException();
            }
        } catch (DetectionFailedException e) {
            out.println("JDK installation skipped: "+e.getMessage());
        }

        return expectedLocation;
    }

    /**
     * Choose the file name suitable for the downloaded JDK bundle.
     */
    private String fileName(Platform p) {
        switch (p) {
        case LINUX:
        case SOLARIS:
            return "jdk.sh";
        case WINDOWS:
            return "jdk.exe";
        }
        throw new AssertionError();
    }

    /**
     * Finds the directory that JDK has created.
     */
    private static final FileFilter JDK_FINDER = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory() && f.getName().startsWith("jdk");
        }
    };

    /**
     * Moves all the contents of this directory into ".."
     */
    private static final FileCallable<Void> PULLUP_DIRECTORY = new FileCallable<Void>() {
        public Void invoke(File f, VirtualChannel channel) throws IOException {
            File p = f.getParentFile();
            for(File child : f.listFiles()) {
                File target = new File(p, child.getName());
                if(!child.renameTo(target))
                    throw new IOException("Failed to rename "+child+" to "+target);
            }
            return null;
        }
    };

    /**
     * Performs a license click through and obtains the one-time URL for downloading bits.
     *
     */
    private URL locate(TaskListener log, Platform platform, CPU cpu) throws IOException {
        final PrintStream out = log.getLogger();

        final WebClient wc = new WebClient();
        wc.setJavaScriptEnabled(false);
        wc.setCssEnabled(false);

        out.println("Visiting http://cds.sun.com/ for download");
        HtmlPage p = (HtmlPage)wc.getPage("https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef="+id);
        HtmlForm form = p.getFormByName("aForm");
        ((HtmlInput)p.getElementById("dnld_license")).click();

        // pick the right download. to  make the comparison more robust, we do it in the upper case
        HtmlOption primary=null,secondary=null;
        HtmlSelect platformChoice = (HtmlSelect) p.getElementById("dnld_platform");
        for(HtmlOption opt : platformChoice.getOptions()) {
            String value = opt.getValueAttribute().toUpperCase(Locale.ENGLISH);
            if(!platform.is(value)) continue;
            switch (cpu.accept(value)) {
            case PRIMARY:   primary = opt;break;
            case SECONDARY: secondary=opt;break;
            case UNACCEPTABLE:  break;
            }
        }
        if(primary==null)   primary=secondary;
        if(primary==null)
            throw new AbortException("Couldn't find the right download for "+platform+" and "+ cpu +" combination");
        ((HtmlSelect)p.getElementById("dnld_platform")).setSelectedAttribute(primary,true);
        p = (HtmlPage)form.submit();

        out.println("Choosing the download bundle");
        List<String> urls = new ArrayList<String>();

        // for some reason, the <style> tag in the middle of the page confuses HtmlUnit,
        // so reverting to text parsing.
        Matcher m = Pattern.compile("<a href=\"(http://cds.sun.com/[^\"]+/VerifyItem-Start[^\"]+)\"").matcher(p.getDocumentElement().getTextContent());
        while(m.find()) {
            String url = m.group(1);
            // still more options to choose from.
            // avoid rpm bundles, and avoid tar.Z bundle
            if(url.contains("rpm"))  continue;
            if(url.contains("tar.Z"))  continue;
            // sparcv9 bundle is add-on to the sparc bundle, so just download 32bit sparc bundle, even on 64bit system
            if(url.contains("sparcv9"))  continue;

            urls.add(url);
            LOGGER.fine("Found a download candidate: "+url);
        }

        // prefer the first match because sometimes "optional downloads" follow the main bundle
        return new URL(urls.get(0));
    }

    public enum Preference {
        PRIMARY, SECONDARY, UNACCEPTABLE
    }

    /**
     * Supported platform.
     */
    public enum Platform {
        LINUX, SOLARIS, WINDOWS;

        public boolean is(String line) {
            return line.contains(name());
        }

        /**
         * Determines the platform of the given node.
         */
        public static Platform of(Node n) throws IOException,InterruptedException,DetectionFailedException {
            return n.toComputer().getChannel().call(new Callable<Platform,DetectionFailedException>() {
                public Platform call() throws DetectionFailedException {
                    return current();
                }
            });
        }

        public static Platform current() throws DetectionFailedException {
            String arch = System.getProperty("os.name").toLowerCase();
            if(arch.contains("linux"))  return LINUX;
            if(arch.contains("windows"))   return WINDOWS;
            if(arch.contains("sun") || arch.contains("solaris"))    return SOLARIS;
            throw new DetectionFailedException("Unknown CPU name: "+arch);
        }
    }

    /**
     * CPU type.
     */
    public enum CPU {
        i386, amd64, Sparc, Itanium;

        /**
         * In JDK5u3, I see platform like "Linux AMD64", while JDK6u3 refers to "Linux x64", so
         * just use "64" for locating bits.
         */
        public Preference accept(String line) {
            switch (this) {
            // these two guys are totally incompatible with everything else, so no fallback
            case Sparc:     return must(line.contains("SPARC"));
            case Itanium:   return must(line.contains("ITANIUM"));

            // 64bit Solaris, Linux, and Windows can all run 32bit executable, so fall back to 32bit if 64bit bundle is not found
            case amd64:
                if(line.contains("64"))     return PRIMARY;
                if(line.contains("SPARC") || line.contains("ITANIUM"))  return UNACCEPTABLE;
                return SECONDARY;
            case i386:
                if(line.contains("64") || line.contains("SPARC") || line.contains("ITANIUM"))     return UNACCEPTABLE;
                return PRIMARY;
            }
            return UNACCEPTABLE;
        }

        private static Preference must(boolean b) {
             return b ? PRIMARY : UNACCEPTABLE;
        }

        /**
         * Determines the CPU of the given node.
         */
        public static CPU of(Node n) throws IOException,InterruptedException, DetectionFailedException {
            return n.toComputer().getChannel().call(new Callable<CPU,DetectionFailedException>() {
                public CPU call() throws DetectionFailedException {
                    return current();
                }
            });
        }

        /**
         * Determines the CPU of the current JVM.
         *
         * http://lopica.sourceforge.net/os.html was useful in writing this code.
         */
        public static CPU current() throws DetectionFailedException {
            String arch = System.getProperty("os.arch").toLowerCase();
            if(arch.contains("sparc"))  return Sparc;
            if(arch.contains("ia64"))   return Itanium;
            if(arch.contains("amd64") || arch.contains("86_64"))    return amd64;
            if(arch.contains("86"))    return i386;
            throw new DetectionFailedException("Unknown CPU architecture: "+arch);
        }
    }

    /**
     * Indicates the failure to detect the OS or CPU.
     */
    private static final class DetectionFailedException extends Exception {
        private DetectionFailedException(String message) {
            super(message);
        }
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<JDKInstaller> {

        public String getDisplayName() {
            return "Install from java.sun.com"; // XXX I18N
        }

        public FormValidation doCheckId(@QueryParameter String value) {
            if (Util.fixEmpty(value) == null) {
                return FormValidation.error("Define JDK ID"); // XXX I18N and improve message
            } else {
                // XXX further checks?
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckAcceptLicense(@QueryParameter boolean value) {
            if (value) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("You must agree to the license to download the JDK."); // XXX I18N
            }
        }

    }

    private static final Logger LOGGER = Logger.getLogger(JDKInstaller.class.getName());
}
