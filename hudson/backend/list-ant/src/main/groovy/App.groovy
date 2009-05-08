import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject

/**
 * Generate a file that lists all Ant releases.
 */
wc = new WebClient()
wc.setJavaScriptEnabled(false);
wc.setCssEnabled(false);
HtmlPage p = (HtmlPage)wc.getPage("http://archive.apache.org/dist/ant/binaries/");

pattern=Pattern.compile("ant-(.+)-bin.zip");

versions = p.getAnchors().findAll{ a -> a.getTextContent().endsWith("-bin.zip")}.collect { HtmlAnchor a ->
    m = pattern.matcher(a.hrefAttribute)
    if(m.find()) {
        ver=m.group(1)
        url = p.getFullyQualifiedUrl(a.hrefAttribute);
        return ["version":ver, "url":url.toExternalForm()]
    }
}

JSONObject envelope = JSONObject.fromObject(["ant": versions])
println envelope.toString(2)

if(project!=null) {
    // if we run from GMaven during a build, put that out in a file as well, with the JSONP support
    File d = new File(project.basedir, "target")
    d.mkdirs()
    new File(d,"ant.json").write("listOfAnts(${envelope.toString()})");
}
