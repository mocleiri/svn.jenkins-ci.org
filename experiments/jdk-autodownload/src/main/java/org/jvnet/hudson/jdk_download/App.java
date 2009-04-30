package org.jvnet.hudson.jdk_download;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import org.apache.commons.io.IOUtils;

/**
 * Download automation of JDK.
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        download("jdk-6u11");
    }

    /**
     *
     * @param code
     *      Product code internally used in http://cds.sun.com/
     *      http://java.sun.com/products/archive/ gives you a good entry point to discover codes
     */
    private static void download(String code) throws IOException {
        WebClient wc = new WebClient();
        wc.setJavaScriptEnabled(false);
        wc.setCssEnabled(false);
        
        HtmlPage p = (HtmlPage)wc.getPage("https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef=" + code + "-oth-JPR@CDS-CDS_Developer");
        HtmlForm form = p.getFormByName("aForm");
        ((HtmlInput)p.getElementById("dnld_license")).click();
        ((HtmlSelect)p.getElementById("dnld_platform")).setSelectedAttribute("Linux x64",true);
        p = (HtmlPage)form.submit();

        List<String> urls = new ArrayList<String>();

        // for some reason, the <style> tag in the middle of the page confuses HtmlUnit,
        // so reverting to text parsing.
        Matcher m = Pattern.compile("<a href=\"([^\"]+/VerifyItem-Start[^\"]+)\"").matcher(p.getDocumentElement().getTextContent());
        while(m.find()) {
            if(m.group(1).contains("rpm"))  continue;
            urls.add(m.group(1));
            System.out.println(m.group(1));
        }

        FileOutputStream out = new FileOutputStream("test.bin");
        InputStream in = new URL(urls.get(0)).openStream();
        IOUtils.copy(in, out);
        in.close();
        out.close();

        // execute this as "echo yes | ./test.bin > install.log"
        // redirect, or else "more" will ask you to read the whole license.
    }
}
