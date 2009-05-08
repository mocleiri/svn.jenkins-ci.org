import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern

/**
 * Generate a file that lists all Ant releases.
 */
wc = new WebClient()
wc.setJavaScriptEnabled(false);
wc.setCssEnabled(false);
HtmlPage p = (HtmlPage)wc.getPage("http://archive.apache.org/dist/ant/binaries/");

pattern=Pattern.compile("ant-(.+)-bin.zip");

p.getAnchors().findAll{ a -> a.getTextContent().endsWith("-bin.zip")}.each { HtmlAnchor a ->
    m = pattern.matcher(a.hrefAttribute)
    if(m.find()) {
        id=m.group(1)
        url = p.getFullyQualifiedUrl(a.hrefAttribute);
        println "${id}\t${url}"
    }
}
