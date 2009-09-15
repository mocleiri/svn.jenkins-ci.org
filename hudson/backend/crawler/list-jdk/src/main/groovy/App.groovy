/**
 * Generate a file that lists all JDK IDs.
 */
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

class Family {
    String name;
    String entryPoint;
}

ID_PATTERN = Pattern.compile("ProductRef=([^&]+)"); // this is how we find a product code

wc = new WebClient()
wc.setJavaScriptEnabled(false);
wc.setCssEnabled(false);
HtmlPage p = getPage("http://java.sun.com/products/archive/");

JSONArray all = new JSONArray();

[
    new Family(name:"JDK 6",entryPoint:"6u12"),
    new Family(name:"JDK 5.0",entryPoint:"5.0_15"),
    new Family(name:"JDK 1.4",entryPoint:"1.4.2_16")
].collect(all) { Family f ->
    return new JSONObject().accumulate("name",f.name).accumulate("list",listFamily(p,f));
}

envelope = new JSONObject().accumulate("jdks", all)
System.out.println(envelope.toString(2));

if(project!=null) {
    // if we run from GMaven during a build, put that out in a file as well, with the JSONP support
    File d = new File(project.basedir, "target")
    d.mkdirs()
    new File(d,"hudson.tools.JDKInstaller.json").write("downloadService.post('hudson.tools.JDKInstaller',${envelope.toString()})");
}

JSONArray listFamily(HtmlPage p, Family f) throws Exception {
    HtmlElement o = p.selectSingleNode("//option[@value='/products/archive/j2se/${f.entryPoint}/index.html']");
    HtmlSelect select = o.getParentNode();

    JSONArray jdks = new JSONArray();

    // the latest JDK6 release is not listed on the archive page.
    if(f.name=="JDK 6") {
        Pattern bareJDK = Pattern.compile("jdk-6u([0-9]+)-oth-JPR");
        String pc = findProductCode(getPage("http://java.sun.com/javase/downloads/").getAnchors().find { HtmlAnchor a ->
            return bareJDK.matcher(a.getHrefAttribute()).find();
        });
        Matcher m = bareJDK.matcher(pc)
        m.find();
        jdks << makeJDK("6 Update "+m.group(1),pc);
    }

    select.getOptions().collect(jdks) { HtmlOption opt ->
        return makeJDK(buildName(opt.getTextContent()),findID(f,opt.getValueAttribute()));
    }
    return jdks;
}

def makeJDK(name,id) {
    return new JSONObject().accumulate("name",name).accumulate("id",id)
}

def buildName(String label) {
    // cut off extra explanation
    int idx = label.indexOf('+');
    if(idx>=0)  label=label.substring(0,idx);
    idx = label.indexOf('(');
    if(idx>=0)  label=label.substring(0,idx);
    return label.trim();
}


def findID(Family f, String href) throws Exception {

    HtmlPage p = getPage("http://java.sun.com${href}");
    HtmlAnchor a = p.getAnchors().find { HtmlAnchor a ->
        def m = ID_PATTERN.matcher(a.getHrefAttribute());
        return m.find() && m.group(1).contains("dk") && !m.group(1).contains("re")
    };

    if(a==null) {
        if (f.name=="JDK 5.0") {
            // JDK5 started hiding links in the survey, so if we can't figure it out,
            // assume the default naming convention
            def m = href =~ /j2se\/5.0_([0-9]+)\/index.html/
            if (m)
                return "jdk-1.5.0_${m.group(1)}-oth-JPR@CDS-CDS_Developer" as String;
        }
        throw new IllegalStateException("No JDK link in "+href);
    }
    return findProductCode(a);
}

def findProductCode(HtmlAnchor a) {
    Matcher m = ID_PATTERN.matcher(a.getHrefAttribute());
    if(m.find())
        return m.group(1);
    throw new IllegalStateException("Failed to find ID for "+href);
}

HtmlPage getPage(String url) {
    print("Fetching ${url} ...")
    long start = System.currentTimeMillis()
    HtmlPage p = wc.getPage(url)
    long end = System.currentTimeMillis()
    println("done (took ${end - start} msec)")
    p
}
