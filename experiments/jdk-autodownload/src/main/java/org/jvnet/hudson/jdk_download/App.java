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
        download("jdk-6u11", Platform.LINUX, Arch.i386);
    }

    /**
     *
     * @param code
     *      Product code internally used in http://cds.sun.com/
     *      http://java.sun.com/products/archive/ gives you a good entry point to discover codes
     */
    private static void download(String code, Platform platform, Arch arch) throws IOException {
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
            System.out.println(url);
        }

        // prefer the first match because sometimes "optional downloads" follow the main bundle
        FileOutputStream out = new FileOutputStream("test.bin");
        InputStream in = new URL(urls.get(0)).openStream();
        IOUtils.copy(in, out);
        in.close();
        out.close();

        // execute this as "echo yes | ./test.bin > install.log"
        // redirect, or else "more" will ask you to read the whole license.
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

        public Preference accept(String line) {
            switch (this) {
            case Sparc:     return must(line.contains("SPARC"));
            case Itanium:   return must(line.contains("ITANIUM"));
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
}
