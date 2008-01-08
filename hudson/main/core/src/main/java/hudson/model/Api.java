package hudson.model;

import hudson.util.IOException2;
import org.dom4j.CharacterData;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * Used to expose remote access API for ".../api/"
 *
 * <p>
 * If the parent object has a <tt>_api.jelly</tt> view, it will be included
 * in the api index page.
 *
 * @author Kohsuke Kawaguchi
 * @see Exported
 */
public class Api extends AbstractModelObject {
    /**
     * Model object to be exposed as XML/JSON/etc.
     */
    public final Object bean;

    public Api(Object bean) {
        this.bean = bean;
    }

    public String getDisplayName() {
        return "API";
    }

    public String getSearchUrl() {
        return "api";
    }

    /**
     * Exposes the bean as XML.
     */
    public void doXml(StaplerRequest req, StaplerResponse rsp, @QueryParameter("xpath") String xpath, @QueryParameter("wrapper") String wrapper) throws IOException, ServletException {
        if(xpath==null) {
            // serve the whole thing
            rsp.serveExposedBean(req,bean,Flavor.XML);
            return;
        }

        StringWriter sw = new StringWriter();

        // first write to String
        Model p = MODEL_BUILDER.get(bean.getClass());
        p.writeTo(bean,Flavor.XML.createDataWriter(bean,sw));

        // apply XPath
        Object result;
        try {
            Document dom = new SAXReader().read(new StringReader(sw.toString()));
            List list = dom.selectNodes(xpath);
            if(list.isEmpty()) {
                // XPath didn't match
                rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                rsp.getWriter().print("XPath "+xpath+" didn't match");
                return;
            }
            if(list.size()>1) {
                if(wrapper!=null) {
                    Element root = DocumentFactory.getInstance().createElement(wrapper);
                    for (Object o : list) {
                        if(o instanceof String)
                            root.addText(o.toString());
                        else
                            root.add(((org.dom4j.Node)o).detach());
                    }
                    result = root;
                } else {
                    // XPath didn't match
                    rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    rsp.getWriter().print(Messages._Api_MultipleMatch(xpath,list.size()));
                    return;
                }
            } else
                result = list.get(0);
        } catch (DocumentException e) {
            throw new IOException2(e);
        }

        if(result instanceof CharacterData) {
            rsp.setContentType("text/plain");
            rsp.getWriter().print(((CharacterData)result).getText());
            return;
        }

        if(result instanceof String) {
            rsp.setContentType("text/plain");
            rsp.getWriter().print(result.toString());
            return;
        }

        // otherwise XML
        rsp.setContentType("application/xml;charset=UTF-8");
        new XMLWriter(rsp.getWriter()).write(result);
    }

    /**
     * Generate schema.
     */
    public void doSchema(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.setContentType("application/xml");
        StreamResult r = new StreamResult(rsp.getOutputStream());
        new SchemaGenerator(new ModelBuilder().get(bean.getClass())).generateSchema(r);
        r.getOutputStream().close();
    }

    /**
     * Exposes the bean as JSON.
     */
    public void doJson(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.serveExposedBean(req,bean, Flavor.JSON);
    }

    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();
}
