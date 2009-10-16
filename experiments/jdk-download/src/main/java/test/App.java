package test;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullWriter;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.w3c.tidy.Tidy;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Can we download JDK without using HtmlUnit?
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        URL url = new URL("https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef=jdk-6u13-oth-JPR@CDS-CDS_Developer");


        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String cookie = con.getHeaderField("Set-Cookie");
        LOGGER.info("Cookie="+cookie);

        Tidy tidy = new Tidy();
        tidy.setErrout(new PrintWriter(new NullWriter()));
        DOMReader domReader = new DOMReader();
        Document dom = domReader.read(tidy.parseDOM(con.getInputStream(), null));

        Element form=null;
        for (Element e : (List<Element>)dom.selectNodes("//form")) {
            String action = e.attributeValue("action");
            LOGGER.info("Found form:"+action);
            if(action.contains("ViewFilteredProducts")) {
                form = e;
                break;
            }
        }

        con = (HttpURLConnection) new URL(form.attributeValue("action")).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Cookie",cookie);
        con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        PrintStream os = new PrintStream(con.getOutputStream());

        // select platform
        Element p = (Element)form.selectSingleNode(".//select[@id='dnld_platform']");
        os.print(p.attributeValue("name")+"=Linux");

        // select language
        Element l = (Element)form.selectSingleNode(".//select[@id='dnld_language']");
        os.print("&"+l.attributeValue("name")+"="+l.element("option").attributeValue("value"));

        // the rest
        for (Element e : (List<Element>)form.selectNodes(".//input")) {
            os.print('&');
            os.print(e.attributeValue("name"));
            os.print('=');
            String value = e.attributeValue("value");
            if(value==null)
                os.print("on"); // assume this is a checkbox
            else
                os.print(URLEncoder.encode(value,"UTF-8"));
        }
        os.close();

        Pattern HREF = Pattern.compile("a href=\"([^\"]+)\"");
        Matcher m = HREF.matcher(IOUtils.toString(con.getInputStream()));
        // this page contains a missing --> that confuses dom4j/jtidy

        while(m.find()) {
            String link = m.group(1);
            if(link.contains("VerifyItem-Start"))
                System.out.println(link);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
}
