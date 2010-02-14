package org.jvnet.hudson.jdk_download;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlOption;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Logger;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import static org.jvnet.hudson.jdk_download.App.Preference.UNACCEPTABLE;
import static org.jvnet.hudson.jdk_download.App.Preference.SECONDARY;
import static org.jvnet.hudson.jdk_download.App.Preference.PRIMARY;

/**
 * Download automation of JDK.
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        testCombinations();
    }

    private static void testCombinations() throws IOException {
        for (Platform p : Platform.values()) {
            for(Arch a : Arch.values()) {
                if(p==Platform.SOLARIS ^ a==Arch.Sparc)  continue;
                System.out.println(p+"&"+a+"\t-> "+locate("jdk-6u11", p, a));
            }
        }
    }

    /**
     * Downloads one thing.
     */
    private static void testOne() throws IOException {
        URL url = locate("jdk-6u11", Platform.LINUX, Arch.i386);

        retrieve(url);

        // execute this as "echo yes | ./test.bin > install.log"
        // redirect, or else "more" will ask you to read the whole license.
    }

    private static void retrieve(URL url) throws IOException {
        FileOutputStream out = new FileOutputStream("test.bin");
        InputStream in = url.openStream();
        IOUtils.copy(in, out);
        in.close();
        out.close();
    }

    /**
     *
     * @param code
     *      Product code internally used in http://cds.sun.com/
     *      http://java.sun.com/products/archive/ gives you a good entry point to discover codes
     */
    private static URL locate(String code, Platform platform, Arch arch) throws IOException {
        WebClient wc = new WebClient();
        wc.setJavaScriptEnabled(false);
        wc.setCssEnabled(false);
        
        HtmlPage p = (HtmlPage)wc.getPage("https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef=" + code + "-oth-JPR@CDS-CDS_Developer");
        HtmlForm form = p.getFormByName("aForm");
        ((HtmlInput)p.getElementById("dnld_license")).click();

        // pick the right download. to  make the comparison more robust, we do it in the upper case
        HtmlOption primary=null,secondary=null;
        HtmlSelect platformChoice = (HtmlSelect) p.getElementById("dnld_platform");
        for(HtmlOption opt : platformChoice.getOptions()) {
            String value = opt.getValueAttribute().toUpperCase(Locale.ENGLISH);
            if(!platform.is(value)) continue;
            switch (arch.accept(value)) {
            case PRIMARY:   primary = opt;break;
            case SECONDARY: secondary=opt;break;
            case UNACCEPTABLE:  break;
            }
        }
        if(primary==null)   primary=secondary;
        if(primary==null)
            throw new IOException("Couldn't find the right download for "+platform+" and "+arch+" combination");
        ((HtmlSelect)p.getElementById("dnld_platform")).setSelectedAttribute(primary,true);
        p = (HtmlPage)form.submit();

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

    public enum Platform {
        LINUX, SOLARIS, WINDOWS;

        public boolean is(String line) {
            return line.contains(name());
        }
    }

    public enum Arch {
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
    }

    private static Preference must(boolean b) {
         return b ? PRIMARY : UNACCEPTABLE;
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
}
